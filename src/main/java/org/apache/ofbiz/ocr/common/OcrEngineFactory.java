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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.ocr.openai.OpenAiOcrEngine;
import org.apache.ofbiz.ocr.tesseract.TesseractOcrEngine;
import org.apache.ofbiz.service.LocalDispatcher;

/**
 * Factory for resolving OCR engine instances.
 */
public final class OcrEngineFactory {

    private static final Map<String, OcrEngine> ENGINES = new ConcurrentHashMap<>();

    private OcrEngineFactory() { }

    public static OcrEngine getEngine(String engineTypeId, Delegator delegator, LocalDispatcher dispatcher) throws OcrException {
        if (UtilValidate.isEmpty(engineTypeId)) {
            throw new OcrException("Engine type is required");
        }

        String key = engineTypeId + "_" + delegator.getDelegatorName();
        OcrEngine engine = ENGINES.get(key);
        if (engine == null) {
            engine = createEngine(engineTypeId, delegator, dispatcher);
            ENGINES.put(key, engine);
        }
        return engine;
    }

    private static OcrEngine createEngine(String engineTypeId, Delegator delegator, LocalDispatcher dispatcher) throws OcrException {
        switch (engineTypeId) {
        case OcrEngineType.TESSERACT:
            return new TesseractOcrEngine(delegator);
        case OcrEngineType.OPENAI:
            return new OpenAiOcrEngine(delegator);
        case OcrEngineType.BOTH:
            return new CompositeOcrEngine(delegator, dispatcher);
        default:
            throw new OcrException("Unsupported OCR engine type: " + engineTypeId);
        }
    }

    /**
     * Composite engine: uses Tesseract for raw text and OpenAI for structured data.
     */
    private static class CompositeOcrEngine implements OcrEngine {

        private final TesseractOcrEngine tesseract;
        private final OpenAiOcrEngine openAi;

        CompositeOcrEngine(Delegator delegator, LocalDispatcher dispatcher) throws OcrException {
            this.tesseract = new TesseractOcrEngine(delegator);
            this.openAi = new OpenAiOcrEngine(delegator);
        }

        @Override
        public OcrResult recognizeText(OcrInput input) throws OcrException {
            return tesseract.recognizeText(input);
        }

        @Override
        public OcrResult recognizeStructured(OcrInput input, OcrMappingDefinition mappingDefinition) throws OcrException {
            return openAi.recognizeStructured(input, mappingDefinition);
        }
    }
}
