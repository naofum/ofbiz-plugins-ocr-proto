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
import java.util.Collections;
import java.util.List;

/**
 * Input data for an OCR engine.
 */
public class OcrInput {

    private List<BufferedImage> images;
    private String mimeType;
    private String fileName;
    private String language;

    public List<BufferedImage> getImages() {
        return images != null ? images : Collections.emptyList();
    }

    public void setImages(List<BufferedImage> images) {
        this.images = images;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isPdf() {
        if (mimeType != null) {
            return mimeType.toLowerCase().contains("pdf");
        }
        if (fileName != null) {
            return fileName.toLowerCase().endsWith(".pdf");
        }
        return false;
    }
}
