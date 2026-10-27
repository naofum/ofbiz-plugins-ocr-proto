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

import java.util.Locale;

/**
 * Determines how an OCR input file can be previewed in the browser.
 */
public final class OcrContentPreview {

    public static final String MODE_IMAGE = "IMAGE";
    public static final String MODE_PDF = "PDF";
    public static final String MODE_OTHER = "OTHER";

    private OcrContentPreview() { }

    /**
     * Returns the preview mode for a MIME type.
     *
     * @param mimeType MIME type of the stored content
     * @return IMAGE, PDF, or OTHER
     */
    public static String mode(String mimeType) {
        if (mimeType == null) {
            return MODE_OTHER;
        }
        String normalized = mimeType.toLowerCase(Locale.ROOT);
        int parameterIndex = normalized.indexOf(';');
        if (parameterIndex >= 0) {
            normalized = normalized.substring(0, parameterIndex).trim();
        }
        if ("application/pdf".equals(normalized)) {
            return MODE_PDF;
        }
        switch (normalized) {
        case "image/png":
        case "image/jpeg":
        case "image/jpg":
        case "image/gif":
        case "image/bmp":
        case "image/webp":
            return MODE_IMAGE;
        default:
            return MODE_OTHER;
        }
    }

    /**
     * Returns true when the content can be shown inline by the browser.
     *
     * @param mimeType MIME type of the stored content
     * @return true for browser-previewable images and PDFs
     */
    public static boolean isInline(String mimeType) {
        return !MODE_OTHER.equals(mode(mimeType));
    }
}
