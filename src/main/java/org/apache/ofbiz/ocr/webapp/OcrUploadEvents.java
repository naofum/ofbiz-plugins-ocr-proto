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
package org.apache.ofbiz.ocr.webapp;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.ocr.common.OcrContentPreview;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Web events for the OCR upload screen.
 */
public final class OcrUploadEvents {

    private static final String MODULE = OcrUploadEvents.class.getName();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private OcrUploadEvents() { }

    /**
     * Runs the upload service and exposes its outputs to the result screen.
     */
    public static String uploadAndRun(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        if (dispatcher == null) {
            request.setAttribute("_ERROR_MESSAGE_", "Local dispatcher is not available");
            return "error";
        }

        Map<String, Object> multiPartMap = UtilGenerics.cast(request.getAttribute("multiPartMap"));
        if (multiPartMap == null) {
            multiPartMap = UtilHttp.getMultiPartParameterMap(request);
        }

        Map<String, Object> context = new HashMap<>();
        Object uploadedFile = multiPartMap.get("uploadedFile");
        if (uploadedFile == null) {
            request.setAttribute("_ERROR_MESSAGE_", "Please select a file to upload");
            return "error";
        }
        context.put("uploadedFile", uploadedFile);
        context.put("_uploadedFile_fileName", multiPartMap.get("_uploadedFile_fileName"));
        context.put("_uploadedFile_contentType", multiPartMap.get("_uploadedFile_contentType"));
        String engineTypeId = (String) getParameter(request, multiPartMap, "engineTypeId");
        String outputMode = (String) getParameter(request, multiPartMap, "outputMode");
        context.put("engineTypeId", engineTypeId);
        context.put("language", getParameter(request, multiPartMap, "language"));
        context.put("persistResult", getParameter(request, multiPartMap, "persistResult"));
        context.put("userLogin", request.getSession().getAttribute("userLogin"));

        String serviceName = "ocrUploadToText";
        if ("STRUCTURED".equalsIgnoreCase(outputMode)) {
            if ("TESSERACT".equalsIgnoreCase(engineTypeId)) {
                request.setAttribute("_ERROR_MESSAGE_", "Structured extraction requires OPENAI or BOTH");
                return "error";
            }
            Object mappingDefinitionId = getParameter(request, multiPartMap, "mappingDefinitionId");
            Object inlinePrompt = getParameter(request, multiPartMap, "inlinePrompt");
            if (UtilValidate.isEmpty(mappingDefinitionId) && UtilValidate.isEmpty(inlinePrompt)) {
                request.setAttribute("_ERROR_MESSAGE_",
                        "Mapping definition or inline prompt is required for structured extraction");
                return "error";
            }
            context.put("mappingDefinitionId", mappingDefinitionId);
            context.put("inlinePrompt", inlinePrompt);
            serviceName = "ocrUploadToStructuredData";
        }

        try {
            Map<String, Object> result = dispatcher.runSync(serviceName, context);
            if (ServiceUtil.isError(result)) {
                Object errorMessage = result.get(ModelService.ERROR_MESSAGE);
                request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                return "error";
            }
            request.setAttribute("rawText", result.get("rawText"));
            String ocrRequestId = (String) result.get("ocrRequestId");
            String contentId = (String) result.get("contentId");
            request.setAttribute("ocrRequestId", ocrRequestId);
            request.setAttribute("contentId", contentId);
            if (UtilValidate.isNotEmpty(ocrRequestId)) {
                request.setAttribute("imageUrl", response.encodeURL(request.getContextPath()
                        + "/control/OcrContentStream?ocrRequestId=" + ocrRequestId));
                request.setAttribute("previewMode", resolvePreviewMode(request, contentId));
            }
            Object structuredResult = result.get("structuredResult");
            if (structuredResult instanceof Map) {
                try {
                    request.setAttribute("structuredResultJson", OBJECT_MAPPER.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(structuredResult));
                } catch (Exception e) {
                    Debug.logWarning(e, "Could not serialize structured result", MODULE);
                    request.setAttribute("structuredResultJson", structuredResult.toString());
                }
            }
            return "success";
        } catch (Exception e) {
            Debug.logError(e, MODULE);
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        }
    }

    private static Object getParameter(HttpServletRequest request, Map<String, Object> multiPartMap, String name) {
        Object value = multiPartMap.get(name);
        if (value == null) {
            value = request.getParameter(name);
        }
        return value;
    }

    private static String resolvePreviewMode(HttpServletRequest request, String contentId) {
        if (UtilValidate.isEmpty(contentId)) {
            return OcrContentPreview.MODE_OTHER;
        }
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        if (delegator == null) {
            return OcrContentPreview.MODE_OTHER;
        }
        try {
            GenericValue content = delegator.findOne("Content",
                    UtilMisc.toMap("contentId", contentId), false);
            if (content == null) {
                return OcrContentPreview.MODE_OTHER;
            }
            return OcrContentPreview.mode(content.getString("mimeTypeId"));
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "Could not determine MIME type for OCR content " + contentId, MODULE);
            return OcrContentPreview.MODE_OTHER;
        }
    }
}
