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
package org.apache.ofbiz.ocr.util;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

/**
 * Utility to convert PDF pages or image files to BufferedImages.
 */
public final class PdfToImageConverter {

    private PdfToImageConverter() {}

    /**
     * Converts a PDF input stream to a list of BufferedImages (one per page).
     */
    public static List<BufferedImage> convert(InputStream inputStream) throws IOException {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFRenderer renderer = new PDFRenderer(document);
            List<BufferedImage> images = new ArrayList<>(document.getNumberOfPages());
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                images.add(renderer.renderImageWithDPI(i, 300));
            }
            return images;
        }
    }

    /**
     * Reads a single image from an input stream.
     */
    public static List<BufferedImage> readImage(InputStream inputStream) throws IOException {
        BufferedImage image = ImageIO.read(inputStream);
        if (image == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(image);
    }
}
