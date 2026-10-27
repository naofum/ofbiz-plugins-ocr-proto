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

/**
 * Common interface for OCR engines.
 */
public interface OcrEngine {

    /**
     * Recognize raw text from the input.
     */
    OcrResult recognizeText(OcrInput input) throws OcrException;

    /**
     * Recognize structured data from the input using the given mapping definition.
     */
    OcrResult recognizeStructured(OcrInput input, OcrMappingDefinition mappingDefinition) throws OcrException;
}
