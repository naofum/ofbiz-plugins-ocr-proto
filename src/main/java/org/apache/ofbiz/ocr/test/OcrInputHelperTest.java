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

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import org.apache.ofbiz.ocr.common.OcrException;
import org.apache.ofbiz.ocr.common.OcrInput;
import org.apache.ofbiz.ocr.common.OcrInputHelper;
import org.apache.ofbiz.service.testtools.OFBizTestCase;

/**
 * Tests for OcrInputHelper.
 */
public final class OcrInputHelperTest extends OFBizTestCase {

    public OcrInputHelperTest(String name) {
        super(name);
    }

    public void testFromBytesPng() throws OcrException, IOException {
        byte[] pngBytes = createPngImage();
        OcrInput input = OcrInputHelper.fromBytes(pngBytes, "test.png", "image/png", "eng");
        assertFalse("Input images should not be empty", input.getImages().isEmpty());
        assertEquals("eng", input.getLanguage());
    }

    public void testFromByteBuffer() throws OcrException, IOException {
        byte[] pngBytes = createPngImage();
        ByteBuffer buffer = ByteBuffer.wrap(pngBytes);
        OcrInput input = OcrInputHelper.fromByteBuffer(buffer, "test.png", "image/png", "jpn");
        assertFalse("Input images should not be empty", input.getImages().isEmpty());
        assertEquals("jpn", input.getLanguage());
    }

    public void testFromEmptyBytes() {
        try {
            OcrInputHelper.fromBytes(new byte[0], "test.png", "image/png", "eng");
            fail("Expected OcrException for empty input");
        } catch (OcrException e) {
            assertTrue("Expected empty input error message", e.getMessage().contains("empty"));
        }
    }

    private byte[] createPngImage() throws IOException {
        BufferedImage image = new BufferedImage(100, 50, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }
}
