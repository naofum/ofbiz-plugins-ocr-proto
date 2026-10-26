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

import org.apache.http.HttpHost;
import org.apache.ofbiz.ocr.common.OcrException;
import org.apache.ofbiz.ocr.openai.OpenAiProxyConfig;
import org.apache.ofbiz.service.testtools.OFBizTestCase;

/**
 * Tests for OpenAiProxyConfig.
 */
public class OpenAiProxyConfigTest extends OFBizTestCase {

    public OpenAiProxyConfigTest(String name) {
        super(name);
    }

    public void testDisabledWhenHostIsEmpty() throws Exception {
        assertFalse(OpenAiProxyConfig.from(null, null, null).isEnabled());
        assertFalse(OpenAiProxyConfig.from("", "8080", "http").isEnabled());
        assertFalse(OpenAiProxyConfig.from("   ", "8080", "http").isEnabled());
    }

    public void testValidProxy() throws Exception {
        OpenAiProxyConfig config = OpenAiProxyConfig.from(" proxy.example.com ", " 8080 ", " HTTP ");
        assertTrue(config.isEnabled());
        assertEquals("proxy.example.com", config.getHost());
        assertEquals(8080, config.getPort());
        assertEquals("http", config.getScheme());

        HttpHost httpHost = config.toHttpHost();
        assertEquals("proxy.example.com", httpHost.getHostName());
        assertEquals(8080, httpHost.getPort());
        assertEquals("http", httpHost.getSchemeName());
    }

    public void testDefaultScheme() throws Exception {
        OpenAiProxyConfig config = OpenAiProxyConfig.from("proxy.example.com", "8080", null);
        assertEquals("http", config.getScheme());
    }

    public void testInvalidPort() {
        assertInvalidProxy("proxy.example.com", "", "http");
        assertInvalidProxy("proxy.example.com", "abc", "http");
        assertInvalidProxy("proxy.example.com", "0", "http");
        assertInvalidProxy("proxy.example.com", "65536", "http");
    }

    public void testInvalidScheme() {
        assertInvalidProxy("proxy.example.com", "8080", "socks5");
    }

    public void testDisabledToHttpHost() throws Exception {
        OpenAiProxyConfig config = OpenAiProxyConfig.from("", "", "");
        try {
            config.toHttpHost();
            fail("Disabled proxy should not create an HttpHost");
        } catch (IllegalStateException expected) {
            // expected
        }
    }

    private void assertInvalidProxy(String host, String port, String scheme) {
        try {
            OpenAiProxyConfig.from(host, port, scheme);
            fail("Expected invalid proxy settings to be rejected");
        } catch (OcrException expected) {
            // expected
        }
    }
}
