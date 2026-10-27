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
package org.apache.ofbiz.ocr.test;

import java.util.List;
import java.util.Map;

import org.apache.ofbiz.ocr.common.OcrMappingDefinition;
import org.apache.ofbiz.service.testtools.OFBizTestCase;

/**
 * Tests for OcrMappingDefinition.
 */
public final class OcrMappingDefinitionTest extends OFBizTestCase {

    public OcrMappingDefinitionTest(String name) {
        super(name);
    }

    public void testParseOutputFields() {
        OcrMappingDefinition def = new OcrMappingDefinition();
        def.setOutputFieldsJson("[{\"name\":\"invoiceNumber\",\"type\":\"String\"},{\"name\":\"totalAmount\",\"type\":\"BigDecimal\"}]");
        List<Map<String, Object>> fields = def.getOutputFields();
        assertEquals("Should parse 2 fields", 2, fields.size());
        assertEquals("invoiceNumber", fields.get(0).get("name"));
        assertEquals("String", fields.get(0).get("type"));
        assertEquals("totalAmount", fields.get(1).get("name"));
        assertEquals("BigDecimal", fields.get(1).get("type"));
    }

    public void testEmptyOutputFields() {
        OcrMappingDefinition def = new OcrMappingDefinition();
        def.setOutputFieldsJson(null);
        List<Map<String, Object>> fields = def.getOutputFields();
        assertEquals("Should return empty list for null JSON", 0, fields.size());
    }
}
