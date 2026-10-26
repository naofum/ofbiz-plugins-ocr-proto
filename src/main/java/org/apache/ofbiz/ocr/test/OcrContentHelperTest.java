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

import java.util.Arrays;

import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.ocr.common.OcrContentHelper;
import org.apache.ofbiz.service.testtools.OFBizTestCase;

/**
 * Tests for OcrContentHelper.
 */
public class OcrContentHelperTest extends OFBizTestCase {

    public OcrContentHelperTest(String name) {
        super(name);
    }

    public void testStoreUploadedFile() throws Exception {
        byte[] data = new byte[] {1, 2, 3, 4, 5};
        String contentId = null;
        String dataResourceId = null;
        try {
            contentId = OcrContentHelper.storeUploadedFile(getDelegator(), data, "test.pdf", "application/pdf", "system");
            assertNotNull("Content ID should be generated", contentId);

            GenericValue content = from("Content").where("contentId", contentId).queryOne();
            assertNotNull("Content should be stored", content);
            assertEquals("DOCUMENT", content.getString("contentTypeId"));
            assertEquals("test.pdf", content.getString("contentName"));
            assertEquals("application/pdf", content.getString("mimeTypeId"));

            dataResourceId = content.getString("dataResourceId");
            assertNotNull("DataResource ID should be set", dataResourceId);

            GenericValue dataResource = from("DataResource").where("dataResourceId", dataResourceId).queryOne();
            assertNotNull("DataResource should be stored", dataResource);
            assertEquals("OTHER_OBJECT", dataResource.getString("dataResourceTypeId"));
            assertEquals("test.pdf", dataResource.getString("dataResourceName"));
            assertEquals("application/pdf", dataResource.getString("mimeTypeId"));

            GenericValue otherDataResource = from("OtherDataResource")
                    .where("dataResourceId", dataResourceId).queryOne();
            assertNotNull("OtherDataResource should be stored", otherDataResource);
            assertTrue("Stored bytes should match",
                    Arrays.equals(data, otherDataResource.getBytes("dataResourceContent")));
        } finally {
            removeContentAndDataResource(contentId, dataResourceId);
        }
    }

    private void removeContentAndDataResource(String contentId, String dataResourceId) throws Exception {
        if (contentId != null) {
            for (GenericValue keyword : from("ContentKeyword").where("contentId", contentId).queryList()) {
                getDelegator().removeValue(keyword);
            }
            GenericValue content = from("Content").where("contentId", contentId).queryOne();
            if (content != null) {
                getDelegator().removeValue(content);
            }
        }
        if (dataResourceId != null) {
            GenericValue otherDataResource = from("OtherDataResource")
                    .where("dataResourceId", dataResourceId).queryOne();
            if (otherDataResource != null) {
                getDelegator().removeValue(otherDataResource);
            }
            GenericValue dataResource = from("DataResource").where("dataResourceId", dataResourceId).queryOne();
            if (dataResource != null) {
                getDelegator().removeValue(dataResource);
            }
        }
    }
}
