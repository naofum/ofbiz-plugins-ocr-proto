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
package org.apache.ofbiz.ocr.common;

import java.util.Collections;
import java.util.Map;

/**
 * Result of an OCR operation.
 */
public final class OcrResult {

    private String rawText;
    private Map<String, Object> structuredResult;
    private Double confidence;
    private String errorMessage;

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public Map<String, Object> getStructuredResult() {
        return structuredResult != null ? structuredResult : Collections.emptyMap();
    }

    public void setStructuredResult(Map<String, Object> structuredResult) {
        this.structuredResult = structuredResult;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean hasError() {
        return errorMessage != null;
    }

    public static OcrResult error(String message) {
        OcrResult result = new OcrResult();
        result.setErrorMessage(message);
        return result;
    }

    public static OcrResult text(String rawText) {
        OcrResult result = new OcrResult();
        result.setRawText(rawText);
        return result;
    }

    public static OcrResult structured(String rawText, Map<String, Object> structuredResult) {
        OcrResult result = new OcrResult();
        result.setRawText(rawText);
        result.setStructuredResult(structuredResult);
        return result;
    }
}
