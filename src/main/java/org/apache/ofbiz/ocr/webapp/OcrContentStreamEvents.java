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
package org.apache.ofbiz.ocr.webapp;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.content.data.DataResourceWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.ocr.common.OcrContentPreview;

/**
 * Streams the original OCR input file for an OCR request.
 */
public final class OcrContentStreamEvents {

    private static final String MODULE = OcrContentStreamEvents.class.getName();

    private OcrContentStreamEvents() {}

    /**
     * Serves the Content/DataResource linked to an OCR request.
     */
    public static String serveContent(HttpServletRequest request, HttpServletResponse response) {
        String ocrRequestId = request.getParameter("ocrRequestId");
        if (UtilValidate.isEmpty(ocrRequestId)) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST);
            return "error";
        }

        Delegator delegator = (Delegator) request.getAttribute("delegator");
        if (delegator == null) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return "error";
        }

        try {
            GenericValue ocrRequest = delegator.findOne("OcrRequest",
                    UtilMisc.toMap("ocrRequestId", ocrRequestId), false);
            if (ocrRequest == null) {
                sendError(response, HttpServletResponse.SC_NOT_FOUND);
                return "error";
            }

            String contentId = ocrRequest.getString("contentId");
            if (UtilValidate.isEmpty(contentId)) {
                sendError(response, HttpServletResponse.SC_NOT_FOUND);
                return "error";
            }

            GenericValue content = delegator.findOne("Content",
                    UtilMisc.toMap("contentId", contentId), false);
            if (content == null) {
                sendError(response, HttpServletResponse.SC_NOT_FOUND);
                return "error";
            }

            String dataResourceId = content.getString("dataResourceId");
            if (UtilValidate.isEmpty(dataResourceId)) {
                sendError(response, HttpServletResponse.SC_NOT_FOUND);
                return "error";
            }

            GenericValue dataResource = delegator.findOne("DataResource",
                    UtilMisc.toMap("dataResourceId", dataResourceId), false);
            if (dataResource == null) {
                sendError(response, HttpServletResponse.SC_NOT_FOUND);
                return "error";
            }

            String mimeType = DataResourceWorker.getMimeType(dataResource);
            if (UtilValidate.isEmpty(mimeType)) {
                mimeType = "application/octet-stream";
            }

            String webSiteId = (String) request.getSession().getAttribute("webSiteId");
            String contextRoot = (String) request.getAttribute("_CONTEXT_ROOT_");
            Locale locale = UtilHttp.getLocale(request);
            String https = request.isSecure() ? "true" : "false";

            Map<String, Object> resourceData = DataResourceWorker.getDataResourceStream(
                    dataResource, https, webSiteId, locale, contextRoot, false);
            InputStream stream = resourceData == null ? null : (InputStream) resourceData.get("stream");
            Long length = resourceData == null ? null : (Long) resourceData.get("length");
            if (stream == null || length == null) {
                sendError(response, HttpServletResponse.SC_NOT_FOUND);
                return "error";
            }

            response.setHeader("Content-Disposition",
                    OcrContentPreview.isInline(mimeType) ? "inline" : "attachment");
            response.setHeader("X-Content-Type-Options", "nosniff");
            UtilHttp.streamContentToBrowser(response, stream, length.intValue(), mimeType);
            return "success";
        } catch (GeneralException | IOException e) {
            Debug.logError(e, "Unable to stream OCR content for request " + ocrRequestId, MODULE);
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return "error";
        } catch (RuntimeException e) {
            Debug.logError(e, "Unable to stream OCR content for request " + ocrRequestId, MODULE);
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return "error";
        }
    }

    private static void sendError(HttpServletResponse response, int status) {
        try {
            if (!response.isCommitted()) {
                response.reset();
                response.sendError(status);
            }
        } catch (IOException | IllegalStateException e) {
            Debug.logWarning(e, "Could not send OCR content stream error", MODULE);
        }
    }
}
