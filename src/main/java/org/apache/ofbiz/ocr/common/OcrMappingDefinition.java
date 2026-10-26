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
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.UtilValidate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Helper for OcrMappingDefinition entity data.
 */
public class OcrMappingDefinition {

    private String mappingDefinitionId;
    private String definitionName;
    private String prompt;
    private String outputFieldsJson;

    public String getMappingDefinitionId() {
        return mappingDefinitionId;
    }

    public void setMappingDefinitionId(String mappingDefinitionId) {
        this.mappingDefinitionId = mappingDefinitionId;
    }

    public String getDefinitionName() {
        return definitionName;
    }

    public void setDefinitionName(String definitionName) {
        this.definitionName = definitionName;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getOutputFieldsJson() {
        return outputFieldsJson;
    }

    public void setOutputFieldsJson(String outputFieldsJson) {
        this.outputFieldsJson = outputFieldsJson;
    }

    /**
     * Parses the outputFields JSON into a list of field definitions.
     * Expected format: [{"name":"amount","type":"BigDecimal"}, ...]
     */
    public List<Map<String, Object>> getOutputFields() {
        if (UtilValidate.isEmpty(outputFieldsJson)) {
            return Collections.emptyList();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(outputFieldsJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
