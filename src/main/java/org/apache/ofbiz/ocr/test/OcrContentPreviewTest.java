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

import org.apache.ofbiz.ocr.common.OcrContentPreview;
import org.apache.ofbiz.service.testtools.OFBizTestCase;

/**
 * Tests for OcrContentPreview.
 */
public class OcrContentPreviewTest extends OFBizTestCase {

    public OcrContentPreviewTest(String name) {
        super(name);
    }

    public void testImageModes() {
        assertEquals(OcrContentPreview.MODE_IMAGE, OcrContentPreview.mode("image/png"));
        assertEquals(OcrContentPreview.MODE_IMAGE, OcrContentPreview.mode("image/jpeg"));
        assertEquals(OcrContentPreview.MODE_IMAGE, OcrContentPreview.mode("IMAGE/WEBP"));
    }

    public void testPdfMode() {
        assertEquals(OcrContentPreview.MODE_PDF, OcrContentPreview.mode("application/pdf"));
    }

    public void testOtherModes() {
        assertEquals(OcrContentPreview.MODE_OTHER, OcrContentPreview.mode("image/tiff"));
        assertEquals(OcrContentPreview.MODE_OTHER, OcrContentPreview.mode("image/svg+xml"));
        assertEquals(OcrContentPreview.MODE_OTHER, OcrContentPreview.mode("text/plain"));
        assertEquals(OcrContentPreview.MODE_OTHER, OcrContentPreview.mode(null));
    }

    public void testIsInline() {
        assertTrue(OcrContentPreview.isInline("image/png"));
        assertTrue(OcrContentPreview.isInline("application/pdf"));
        assertFalse(OcrContentPreview.isInline("image/tiff"));
        assertFalse(OcrContentPreview.isInline(null));
    }
}
