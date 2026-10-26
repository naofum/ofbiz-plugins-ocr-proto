/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ofbiz.ocr.openai;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.ocr.common.OcrEngine;
import org.apache.ofbiz.ocr.common.OcrEngineType;
import org.apache.ofbiz.ocr.common.OcrException;
import org.apache.ofbiz.ocr.common.OcrInput;
import org.apache.ofbiz.ocr.common.OcrMappingDefinition;
import org.apache.ofbiz.ocr.common.OcrResult;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * OCR engine implementation using an OpenAI compatible chat completion API.
 */
public class OpenAiOcrEngine implements OcrEngine {

    private static final String MODULE = OpenAiOcrEngine.class.getName();
    private static final String RESOURCE = "ocr";
    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    private static final String DEFAULT_MODEL = "gpt-4o";

    private final Delegator delegator;
    private final ObjectMapper objectMapper;

    public OpenAiOcrEngine(Delegator delegator) {
        this.delegator = delegator;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public OcrResult recognizeText(OcrInput input) throws OcrException {
        OcrMappingDefinition mapping = new OcrMappingDefinition();
        mapping.setPrompt("Extract all text from this image. Return the result as plain text.");
        return recognizeStructured(input, mapping);
    }

    @Override
    public OcrResult recognizeStructured(OcrInput input, OcrMappingDefinition mappingDefinition) throws OcrException {
        if (input.getImages().isEmpty()) {
            return OcrResult.error("No images to process");
        }
        if (mappingDefinition == null || UtilValidate.isEmpty(mappingDefinition.getPrompt())) {
            return OcrResult.error("Mapping definition or inline prompt is required for structured extraction");
        }

        OpenAiConfig config = resolveConfig();

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = createRequest(config, mappingDefinition, input);
            HttpResponse response = client.execute(request);
            return parseResponse(response);
        } catch (IOException e) {
            Debug.logError(e, MODULE);
            throw new OcrException("OpenAI API call failed", e);
        }
    }

    private HttpPost createRequest(OpenAiConfig config, OcrMappingDefinition mappingDefinition, OcrInput input) throws IOException, OcrException {
        HttpPost request = new HttpPost(config.baseUrl + "/chat/completions");
        request.setHeader("Content-Type", "application/json");
        if (UtilValidate.isNotEmpty(config.apiKey)) {
            request.setHeader("Authorization", "Bearer " + config.apiKey);
        }

        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", config.model);

        ArrayNode messages = root.putArray("messages");
        ObjectNode message = messages.addObject();
        message.put("role", "user");

        ArrayNode content = message.putArray("content");
        ObjectNode textContent = content.addObject();
        textContent.put("type", "text");
        textContent.put("text", buildPrompt(mappingDefinition));

        for (BufferedImage image : input.getImages()) {
            ObjectNode imageContent = content.addObject();
            imageContent.put("type", "image_url");
            ObjectNode imageUrl = imageContent.putObject("image_url");
            imageUrl.put("url", "data:image/png;base64," + encodeImage(image));
        }

        String json = objectMapper.writeValueAsString(root);
        request.setEntity(new StringEntity(json, StandardCharsets.UTF_8));
        return request;
    }

    private String buildPrompt(OcrMappingDefinition mappingDefinition) {
        String prompt = mappingDefinition.getPrompt();
        List<Map<String, Object>> outputFields = mappingDefinition.getOutputFields();
        if (UtilValidate.isNotEmpty(outputFields)) {
            prompt += "\n\nOutput fields:\n";
            for (Map<String, Object> field : outputFields) {
                prompt += "- " + field.get("name") + " (" + field.get("type") + ")\n";
            }
        }
        prompt += "\nReturn the result as a JSON object.";
        return prompt;
    }

    private String encodeImage(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private OcrResult parseResponse(HttpResponse response) throws IOException, OcrException {
        HttpEntity entity = response.getEntity();
        String body = entity != null ? EntityUtils.toString(entity, StandardCharsets.UTF_8) : "";

        if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300) {
            throw new OcrException("OpenAI API returned HTTP " + response.getStatusLine().getStatusCode() + ": " + body);
        }

        JsonNode root = objectMapper.readTree(body);
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.size() == 0) {
            throw new OcrException("OpenAI API response did not contain choices: " + body);
        }

        String content = choices.get(0).path("message").path("content").asText();
        if (UtilValidate.isEmpty(content)) {
            return OcrResult.text("");
        }

        Map<String, Object> structuredResult = parseStructuredContent(content);
        return OcrResult.structured(content, structuredResult);
    }

    private Map<String, Object> parseStructuredContent(String content) {
        String cleaned = content.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        cleaned = cleaned.trim();

        try {
            return objectMapper.readValue(cleaned, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            Debug.logWarning("Could not parse OpenAI response as JSON: " + content, MODULE);
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("rawResponse", content);
            return fallback;
        }
    }

    private OpenAiConfig resolveConfig() throws OcrException {
        OpenAiConfig config = new OpenAiConfig();
        config.baseUrl = UtilProperties.getPropertyValue(RESOURCE, "ocr.openai.baseUrl", DEFAULT_BASE_URL);
        config.model = UtilProperties.getPropertyValue(RESOURCE, "ocr.openai.model", DEFAULT_MODEL);

        try {
            GenericValue cfg = EntityQuery.use(delegator)
                    .from("OcrEngineConfig")
                    .where("engineTypeId", OcrEngineType.OPENAI)
                    .orderBy("createdTxStamp")
                    .queryFirst();
            if (cfg != null) {
                if (UtilValidate.isNotEmpty(cfg.getString("baseUrl"))) {
                    config.baseUrl = cfg.getString("baseUrl");
                }
                if (UtilValidate.isNotEmpty(cfg.getString("apiKey"))) {
                    config.apiKey = cfg.getString("apiKey");
                }
                if (UtilValidate.isNotEmpty(cfg.getString("modelName"))) {
                    config.model = cfg.getString("modelName");
                }
            }
        } catch (Exception e) {
            Debug.logWarning(e, "Could not resolve OpenAI config", MODULE);
        }

        if (UtilValidate.isEmpty(config.apiKey)) {
            throw new OcrException("OpenAI API key is not configured");
        }

        return config;
    }

    private static class OpenAiConfig {
        String baseUrl;
        String apiKey;
        String model;
    }
}
