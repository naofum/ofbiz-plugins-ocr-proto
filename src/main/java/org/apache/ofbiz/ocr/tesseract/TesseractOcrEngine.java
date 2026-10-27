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
package org.apache.ofbiz.ocr.tesseract;

import java.awt.image.BufferedImage;

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

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

/**
 * OCR engine implementation using Tesseract via Tess4J.
 */
public class TesseractOcrEngine implements OcrEngine {

    private static final String MODULE = TesseractOcrEngine.class.getName();
    private static final String RESOURCE = "ocr";

    private final Delegator delegator;

    public TesseractOcrEngine(Delegator delegator) {
        this.delegator = delegator;
    }

    @Override
    public OcrResult recognizeText(OcrInput input) throws OcrException {
        if (input.getImages().isEmpty()) {
            return OcrResult.error("No images to process");
        }

        ITesseract tesseract = createTesseract(input);
        StringBuilder textBuilder = new StringBuilder();
        Double confidence = null;

        for (int i = 0; i < input.getImages().size(); i++) {
            BufferedImage image = input.getImages().get(i);
            try {
                String pageText = tesseract.doOCR(image);
                if (i > 0) {
                    textBuilder.append("\n\n--- Page ").append(i + 1).append(" ---\n\n");
                }
                textBuilder.append(pageText);
            } catch (TesseractException e) {
                Debug.logError(e, MODULE);
                throw new OcrException("Tesseract OCR failed on page " + (i + 1), e);
            }
        }

        OcrResult result = OcrResult.text(textBuilder.toString());
        result.setConfidence(confidence);
        return result;
    }

    @Override
    public OcrResult recognizeStructured(OcrInput input, OcrMappingDefinition mappingDefinition) throws OcrException {
        return OcrResult.error("Tesseract engine does not support structured data extraction. Use OPENAI or BOTH.");
    }

    private ITesseract createTesseract(OcrInput input) throws OcrException {
        Tesseract tesseract = new Tesseract();

        String dataPath = resolveDataPath();
        if (UtilValidate.isNotEmpty(dataPath)) {
            tesseract.setDatapath(dataPath);
        }

        String language = UtilValidate.isNotEmpty(input.getLanguage()) ? input.getLanguage()
                : UtilProperties.getPropertyValue(RESOURCE, "ocr.tesseract.language", "eng");
        tesseract.setLanguage(language);

        return tesseract;
    }

    private String resolveDataPath() throws OcrException {
        try {
            GenericValue config = EntityQuery.use(delegator)
                    .from("OcrEngineConfig")
                    .where("engineTypeId", OcrEngineType.TESSERACT)
                    .orderBy("createdTxStamp")
                    .queryFirst();
            if (config != null && UtilValidate.isNotEmpty(config.getString("tessDataPath"))) {
                return config.getString("tessDataPath");
            }
        } catch (Exception e) {
            Debug.logWarning(e, "Could not resolve Tesseract config", MODULE);
        }
        return UtilProperties.getPropertyValue(RESOURCE, "ocr.tesseract.dataPath", "");
    }
}
