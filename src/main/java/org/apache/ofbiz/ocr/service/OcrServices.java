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
package org.apache.ofbiz.ocr.service;

import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.content.data.DataResourceWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.ocr.common.OcrContentHelper;
import org.apache.ofbiz.ocr.common.OcrEngine;
import org.apache.ofbiz.ocr.common.OcrEngineFactory;
import org.apache.ofbiz.ocr.common.OcrException;
import org.apache.ofbiz.ocr.common.OcrInput;
import org.apache.ofbiz.ocr.common.OcrInputHelper;
import org.apache.ofbiz.ocr.common.OcrMappingDefinition;
import org.apache.ofbiz.ocr.common.OcrResult;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * OFBiz service entry points for OCR operations.
 */
public class OcrServices {

    private static final String MODULE = OcrServices.class.getName();
    private static final String RESOURCE = "ocr";

    public static Map<String, Object> ocrImageToText(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        String contentId = (String) context.get("contentId");
        String engineTypeId = (String) context.get("engineTypeId");
        String language = (String) context.get("language");
        String persistResult = (String) context.get("persistResult");

        try {
            OcrInput input = OcrInputHelper.fromContentId(delegator, contentId, language);
            OcrResult result = executeEngine(dctx, input, engineTypeId, null);
            if (result.hasError()) {
                return ServiceUtil.returnError(result.getErrorMessage());
            }
            String ocrRequestId = persistRequest(delegator, context, input, result, "OCRREQ_SUCCESS", persistResult);
            return buildTextResult(ocrRequestId, result);
        } catch (Exception e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    public static Map<String, Object> ocrUploadToText(DispatchContext dctx, Map<String, Object> context) {
        String contentId;
        try {
            contentId = storeUploadedFileContent(dctx, context);
        } catch (OcrException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        Map<String, Object> ocrContext = new HashMap<>();
        ocrContext.put("contentId", contentId);
        ocrContext.put("engineTypeId", context.get("engineTypeId"));
        ocrContext.put("language", context.get("language"));
        ocrContext.put("persistResult", context.get("persistResult"));
        ocrContext.put("userLogin", context.get("userLogin"));
        return runOcrService(dctx, "ocrImageToText", ocrContext, contentId);
    }

    public static Map<String, Object> ocrUploadToStructuredData(DispatchContext dctx, Map<String, Object> context) {
        String contentId;
        try {
            contentId = storeUploadedFileContent(dctx, context);
        } catch (OcrException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        Map<String, Object> ocrContext = new HashMap<>();
        ocrContext.put("contentId", contentId);
        ocrContext.put("engineTypeId", context.get("engineTypeId"));
        ocrContext.put("mappingDefinitionId", context.get("mappingDefinitionId"));
        ocrContext.put("inlinePrompt", context.get("inlinePrompt"));
        ocrContext.put("outputFields", context.get("outputFields"));
        ocrContext.put("persistResult", context.get("persistResult"));
        ocrContext.put("userLogin", context.get("userLogin"));
        return runOcrService(dctx, "ocrImageToStructuredData", ocrContext, contentId);
    }

    private static String storeUploadedFileContent(DispatchContext dctx, Map<String, Object> context)
            throws OcrException {
        Delegator delegator = dctx.getDelegator();
        ByteBuffer uploadedFile = (ByteBuffer) context.get("uploadedFile");
        if (uploadedFile == null) {
            throw new OcrException("Uploaded file is required");
        }

        byte[] data = new byte[uploadedFile.remaining()];
        uploadedFile.duplicate().get(data);
        if (data.length == 0) {
            throw new OcrException("Uploaded file is empty");
        }

        String fileName = (String) context.get("_uploadedFile_fileName");
        String mimeType = (String) context.get("_uploadedFile_contentType");
        if (UtilValidate.isEmpty(mimeType) || "application/octet-stream".equalsIgnoreCase(mimeType)) {
            try {
                String detected = DataResourceWorker.getMimeTypeWithByteBuffer(ByteBuffer.wrap(data));
                if (UtilValidate.isNotEmpty(detected)) {
                    mimeType = detected;
                }
            } catch (Exception e) {
                Debug.logWarning(e, "Could not detect MIME type for uploaded file", MODULE);
            }
        }

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String userLoginId = userLogin != null ? userLogin.getString("userLoginId") : null;
        return OcrContentHelper.storeUploadedFile(delegator, data, fileName, mimeType, userLoginId);
    }

    private static Map<String, Object> runOcrService(DispatchContext dctx, String serviceName,
            Map<String, Object> ocrContext, String contentId) {
        try {
            Map<String, Object> result = dctx.getDispatcher().runSync(serviceName, ocrContext);
            if (ServiceUtil.isError(result)) {
                return result;
            }
            Map<String, Object> output = new HashMap<>(result);
            output.put("contentId", contentId);
            return output;
        } catch (Exception e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError("OCR failed: " + e.getMessage());
        }
    }

    public static Map<String, Object> ocrBytesToText(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        ByteBuffer byteBuffer = (ByteBuffer) context.get("byteBuffer");
        String fileName = (String) context.get("fileName");
        String engineTypeId = (String) context.get("engineTypeId");
        String language = (String) context.get("language");
        String persistResult = (String) context.get("persistResult");

        try {
            OcrInput input = OcrInputHelper.fromByteBuffer(byteBuffer, fileName, null, language);
            OcrResult result = executeEngine(dctx, input, engineTypeId, null);
            if (result.hasError()) {
                return ServiceUtil.returnError(result.getErrorMessage());
            }
            String ocrRequestId = persistRequest(delegator, context, input, result, "OCRREQ_SUCCESS", persistResult);
            return buildTextResult(ocrRequestId, result);
        } catch (Exception e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    public static Map<String, Object> ocrImageToStructuredData(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        String contentId = (String) context.get("contentId");
        String engineTypeId = (String) context.get("engineTypeId");
        String mappingDefinitionId = (String) context.get("mappingDefinitionId");
        String inlinePrompt = (String) context.get("inlinePrompt");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> outputFields = (List<Map<String, Object>>) context.get("outputFields");
        String persistResult = (String) context.get("persistResult");

        try {
            OcrMappingDefinition mapping = resolveMappingDefinition(delegator, mappingDefinitionId, inlinePrompt, outputFields);
            OcrInput input = OcrInputHelper.fromContentId(delegator, contentId, null);
            OcrResult result = executeEngine(dctx, input, engineTypeId, mapping);
            if (result.hasError()) {
                return ServiceUtil.returnError(result.getErrorMessage());
            }
            String ocrRequestId = persistRequest(delegator, context, input, result, "OCRREQ_SUCCESS", persistResult);
            return buildStructuredResult(ocrRequestId, result);
        } catch (Exception e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    public static Map<String, Object> ocrBytesToStructuredData(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        ByteBuffer byteBuffer = (ByteBuffer) context.get("byteBuffer");
        String fileName = (String) context.get("fileName");
        String engineTypeId = (String) context.get("engineTypeId");
        String mappingDefinitionId = (String) context.get("mappingDefinitionId");
        String inlinePrompt = (String) context.get("inlinePrompt");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> outputFields = (List<Map<String, Object>>) context.get("outputFields");
        String persistResult = (String) context.get("persistResult");

        try {
            OcrMappingDefinition mapping = resolveMappingDefinition(delegator, mappingDefinitionId, inlinePrompt, outputFields);
            OcrInput input = OcrInputHelper.fromByteBuffer(byteBuffer, fileName, null, null);
            OcrResult result = executeEngine(dctx, input, engineTypeId, mapping);
            if (result.hasError()) {
                return ServiceUtil.returnError(result.getErrorMessage());
            }
            String ocrRequestId = persistRequest(delegator, context, input, result, "OCRREQ_SUCCESS", persistResult);
            return buildStructuredResult(ocrRequestId, result);
        } catch (Exception e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    private static OcrResult executeEngine(DispatchContext dctx, OcrInput input, String engineTypeId,
            OcrMappingDefinition mappingDefinition) throws OcrException {
        if (UtilValidate.isEmpty(engineTypeId)) {
            throw new OcrException("engineTypeId is required");
        }

        OcrEngine engine = OcrEngineFactory.getEngine(engineTypeId, dctx.getDelegator(), dctx.getDispatcher());
        if (mappingDefinition != null) {
            return engine.recognizeStructured(input, mappingDefinition);
        }
        return engine.recognizeText(input);
    }

    private static OcrMappingDefinition resolveMappingDefinition(Delegator delegator, String mappingDefinitionId,
            String inlinePrompt, List<Map<String, Object>> outputFields) throws OcrException {
        if (UtilValidate.isNotEmpty(mappingDefinitionId)) {
            try {
                GenericValue value = EntityQuery.use(delegator).from("OcrMappingDefinition")
                        .where("ocrMappingDefinitionId", mappingDefinitionId).queryOne();
                if (value == null) {
                    throw new OcrException("Mapping definition not found: " + mappingDefinitionId);
                }
                OcrMappingDefinition def = new OcrMappingDefinition();
                def.setMappingDefinitionId(value.getString("ocrMappingDefinitionId"));
                def.setDefinitionName(value.getString("definitionName"));
                def.setPrompt(value.getString("prompt"));
                def.setOutputFieldsJson(value.getString("outputFields"));
                return def;
            } catch (GenericEntityException e) {
                throw new OcrException("Failed to load mapping definition", e);
            }
        }

        if (UtilValidate.isEmpty(inlinePrompt)) {
            throw new OcrException("Either mappingDefinitionId or inlinePrompt is required");
        }
        OcrMappingDefinition def = new OcrMappingDefinition();
        def.setPrompt(inlinePrompt);
        if (UtilValidate.isNotEmpty(outputFields)) {
            try {
                def.setOutputFieldsJson(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(outputFields));
            } catch (Exception e) {
                Debug.logWarning(e, "Could not serialize outputFields", MODULE);
            }
        }
        return def;
    }

    private static String persistRequest(Delegator delegator, Map<String, Object> context, OcrInput input,
            OcrResult result, String statusId, String persistResult) throws GenericEntityException {
        String persist = UtilValidate.isNotEmpty(persistResult) ? persistResult
                : UtilProperties.getPropertyValue(RESOURCE, "ocr.request.defaultPersistResult", "N");

        Timestamp now = UtilDateTime.nowTimestamp();
        String ocrRequestId = delegator.getNextSeqId("OcrRequest");
        GenericValue request = delegator.makeValue("OcrRequest");
        request.set("ocrRequestId", ocrRequestId);
        request.set("contentId", context.get("contentId"));
        request.set("engineTypeId", context.get("engineTypeId"));
        request.set("mappingDefinitionId", context.get("mappingDefinitionId"));
        request.set("inlinePrompt", context.get("inlinePrompt"));
        request.set("language", input.getLanguage());
        request.set("statusId", statusId);
        request.set("persistResult", persist);
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        request.set("createdByUserLoginId", userLogin != null ? userLogin.getString("userLoginId") : context.get("userLoginId"));
        request.set("createdTxStamp", now);
        request.set("lastUpdatedTxStamp", now);
        delegator.create(request);

        if ("Y".equalsIgnoreCase(persist)) {
            String resultId = delegator.getNextSeqId("OcrRequestResult");
            GenericValue resultValue = delegator.makeValue("OcrRequestResult");
            resultValue.set("ocrRequestResultId", resultId);
            resultValue.set("ocrRequestId", ocrRequestId);
            resultValue.set("rawText", result.getRawText());
            if (result.getStructuredResult() != null && !result.getStructuredResult().isEmpty()) {
                try {
                    resultValue.set("structuredResult", new com.fasterxml.jackson.databind.ObjectMapper()
                            .writeValueAsString(result.getStructuredResult()));
                } catch (Exception e) {
                    Debug.logWarning(e, "Could not serialize structured result", MODULE);
                }
            }
            resultValue.set("confidence", result.getConfidence());
            resultValue.set("errorMessage", result.getErrorMessage());
            resultValue.set("createdTxStamp", now);
            resultValue.set("lastUpdatedTxStamp", now);
            delegator.create(resultValue);
        }

        return ocrRequestId;
    }

    private static Map<String, Object> buildTextResult(String ocrRequestId, OcrResult result) {
        Map<String, Object> output = new HashMap<>();
        output.put("ocrRequestId", ocrRequestId);
        output.put("rawText", result.getRawText());
        if (result.hasError()) {
            output.put("errorMessage", result.getErrorMessage());
        }
        return output;
    }

    private static Map<String, Object> buildStructuredResult(String ocrRequestId, OcrResult result) {
        Map<String, Object> output = new HashMap<>();
        output.put("ocrRequestId", ocrRequestId);
        output.put("rawText", result.getRawText());
        output.put("structuredResult", result.getStructuredResult());
        if (result.hasError()) {
            output.put("errorMessage", result.getErrorMessage());
        }
        return output;
    }
}
