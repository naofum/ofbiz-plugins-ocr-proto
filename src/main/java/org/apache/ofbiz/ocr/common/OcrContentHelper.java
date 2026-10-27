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

import java.sql.Timestamp;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;

/**
 * Helper for storing an uploaded OCR input as a Content/DataResource pair.
 */
public final class OcrContentHelper {

    private static final String MODULE = OcrContentHelper.class.getName();

    private OcrContentHelper() { }

    /**
     * Stores binary data as a DataResource (OTHER_OBJECT) and Content record.
     *
     * @param delegator OFBiz delegator
     * @param data uploaded file bytes
     * @param fileName original file name
     * @param mimeType MIME type of the file
     * @param userLoginId user login ID, may be null
     * @return the created Content ID
     * @throws OcrException when the records cannot be created
     */
    public static String storeUploadedFile(Delegator delegator, byte[] data, String fileName, String mimeType,
            String userLoginId) throws OcrException {
        if (data == null || data.length == 0) {
            throw new OcrException("Uploaded file is empty");
        }
        if (UtilValidate.isEmpty(fileName)) {
            fileName = "upload";
        }
        if (UtilValidate.isEmpty(mimeType)) {
            mimeType = "application/octet-stream";
        }
        if ("application/octet-stream".equalsIgnoreCase(mimeType) && fileName.toLowerCase().endsWith(".pdf")) {
            mimeType = "application/pdf";
        }

        Timestamp now = UtilDateTime.nowTimestamp();
        String dataResourceId = delegator.getNextSeqId("DataResource");
        String contentId = delegator.getNextSeqId("Content");

        try {
            GenericValue dataResource = delegator.makeValue("DataResource");
            dataResource.set("dataResourceId", dataResourceId);
            dataResource.set("dataResourceTypeId", "OTHER_OBJECT");
            dataResource.set("dataTemplateTypeId", "NONE");
            dataResource.set("dataResourceName", fileName);
            dataResource.set("mimeTypeId", mimeType);
            dataResource.set("createdDate", now);
            dataResource.set("lastModifiedDate", now);
            dataResource.set("createdByUserLogin", userLoginId);
            dataResource.set("lastModifiedByUserLogin", userLoginId);
            delegator.create(dataResource);

            GenericValue otherDataResource = delegator.makeValue("OtherDataResource");
            otherDataResource.set("dataResourceId", dataResourceId);
            otherDataResource.setBytes("dataResourceContent", data);
            delegator.create(otherDataResource);

            GenericValue content = delegator.makeValue("Content");
            content.set("contentId", contentId);
            content.set("contentTypeId", "DOCUMENT");
            content.set("dataResourceId", dataResourceId);
            content.set("contentName", fileName);
            content.set("mimeTypeId", mimeType);
            content.set("statusId", "CTNT_IN_PROGRESS");
            content.set("createdDate", now);
            content.set("lastModifiedDate", now);
            content.set("createdByUserLogin", userLoginId);
            content.set("lastModifiedByUserLogin", userLoginId);
            delegator.create(content);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            throw new OcrException("Failed to store uploaded file as Content", e);
        }

        return contentId;
    }
}
