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

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.content.data.DataResourceWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.ocr.util.PdfToImageConverter;

/**
 * Helper for building OcrInput from various sources.
 */
public class OcrInputHelper {

    private static final String MODULE = OcrInputHelper.class.getName();

    private OcrInputHelper() {}

    public static OcrInput fromContentId(Delegator delegator, String contentId, String language) throws OcrException {
        if (UtilValidate.isEmpty(contentId)) {
            throw new OcrException("contentId is required");
        }
        try {
            GenericValue content = EntityQuery.use(delegator).from("Content").where("contentId", contentId).queryOne();
            if (content == null) {
                throw new OcrException("Content not found: " + contentId);
            }
            GenericValue dataResource = content.getRelatedOne("DataResource", false);
            if (dataResource == null) {
                throw new OcrException("DataResource not found for content: " + contentId);
            }
            byte[] data = readDataResourceBytes(dataResource);
            if (data == null || data.length == 0) {
                throw new OcrException("No data found for content: " + contentId);
            }
            String fileName = dataResource.getString("dataResourceName");
            String mimeType = DataResourceWorker.getDataResourceMimeType(delegator, dataResource.getString("dataResourceId"), null);
            return fromBytes(data, fileName, mimeType, language);
        } catch (RuntimeException | GenericEntityException e) {
            Debug.logError(e, MODULE);
            throw new OcrException("Failed to read content: " + contentId, e);
        }
    }

    public static OcrInput fromByteBuffer(ByteBuffer byteBuffer, String fileName, String mimeType, String language) throws OcrException {
        if (byteBuffer == null) {
            throw new OcrException("byteBuffer is required");
        }
        byte[] data = new byte[byteBuffer.remaining()];
        byteBuffer.get(data);
        return fromBytes(data, fileName, mimeType, language);
    }

    public static OcrInput fromBytes(byte[] data, String fileName, String mimeType, String language) throws OcrException {
        if (data == null || data.length == 0) {
            throw new OcrException("Input data is empty");
        }

        OcrInput input = new OcrInput();
        input.setFileName(fileName);
        input.setMimeType(mimeType);
        input.setLanguage(language);

        try {
            List<BufferedImage> images;
            if (isPdf(fileName, mimeType)) {
                images = PdfToImageConverter.convert(new java.io.ByteArrayInputStream(data));
            } else {
                images = PdfToImageConverter.readImage(new java.io.ByteArrayInputStream(data));
            }
            input.setImages(images);
        } catch (IOException e) {
            throw new OcrException("Failed to process input file", e);
        }

        return input;
    }

    private static byte[] readDataResourceBytes(GenericValue dataResource) throws OcrException {
        try {
            Map<String, Object> streamResult = DataResourceWorker.getDataResourceStream(dataResource, null, null, Locale.getDefault(), null, false);
            InputStream stream = (InputStream) streamResult.get("stream");
            if (stream == null) {
                throw new OcrException("DataResource stream is null");
            }
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = stream.read(buffer)) != -1) {
                    baos.write(buffer, 0, read);
                }
                return baos.toByteArray();
            } finally {
                stream.close();
            }
        } catch (Exception e) {
            throw new OcrException("Failed to read DataResource bytes", e);
        }
    }

    private static boolean isPdf(String fileName, String mimeType) {
        if (UtilValidate.isNotEmpty(mimeType)) {
            return mimeType.toLowerCase().contains("pdf");
        }
        if (UtilValidate.isNotEmpty(fileName)) {
            return fileName.toLowerCase().endsWith(".pdf");
        }
        return false;
    }
}
