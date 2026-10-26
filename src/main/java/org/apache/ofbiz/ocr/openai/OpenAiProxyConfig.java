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
package org.apache.ofbiz.ocr.openai;

import java.util.Locale;

import org.apache.http.HttpHost;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.ocr.common.OcrException;

/**
 * Proxy configuration used only for OpenAI-compatible API calls.
 */
public final class OpenAiProxyConfig {

    private static final OpenAiProxyConfig DISABLED = new OpenAiProxyConfig(null, 0, null);

    private final String host;
    private final int port;
    private final String scheme;

    private OpenAiProxyConfig(String host, int port, String scheme) {
        this.host = host;
        this.port = port;
        this.scheme = scheme;
    }

    /**
     * Builds a proxy configuration from OCR properties.
     *
     * @param host proxy host; empty disables the proxy
     * @param port proxy port
     * @param scheme proxy scheme, defaults to http
     * @return proxy configuration
     * @throws OcrException when the proxy settings are invalid
     */
    public static OpenAiProxyConfig from(String host, String port, String scheme) throws OcrException {
        String normalizedHost = host == null ? null : host.trim();
        if (UtilValidate.isEmpty(normalizedHost)) {
            return DISABLED;
        }

        String normalizedPort = port == null ? "" : port.trim();
        if (UtilValidate.isEmpty(normalizedPort)) {
            throw new OcrException("ocr.openai.proxyPort is required when ocr.openai.proxyHost is set");
        }

        int parsedPort;
        try {
            parsedPort = Integer.parseInt(normalizedPort);
        } catch (NumberFormatException e) {
            throw new OcrException("ocr.openai.proxyPort must be a number", e);
        }
        if (parsedPort < 1 || parsedPort > 65535) {
            throw new OcrException("ocr.openai.proxyPort must be between 1 and 65535");
        }

        String normalizedScheme = scheme == null ? "" : scheme.trim().toLowerCase(Locale.ROOT);
        if (UtilValidate.isEmpty(normalizedScheme)) {
            normalizedScheme = "http";
        }
        if (!"http".equals(normalizedScheme) && !"https".equals(normalizedScheme)) {
            throw new OcrException("ocr.openai.proxyScheme must be http or https");
        }

        return new OpenAiProxyConfig(normalizedHost, parsedPort, normalizedScheme);
    }

    public boolean isEnabled() {
        return host != null;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getScheme() {
        return scheme;
    }

    public HttpHost toHttpHost() {
        if (!isEnabled()) {
            throw new IllegalStateException("Proxy is not enabled");
        }
        return new HttpHost(host, port, scheme);
    }
}
