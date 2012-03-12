/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.Cookie;
import org.junit.Rule;
import org.junit.Test;


public class HttpResponseTestCase extends FunctionalTestCase
{
    private static final String HTTP_BODY = "<html><head></head><body><p>This is the response body</p></body></html>";

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "http-response-conf.xml";
    }

    @Test
    public void testHttpResponseError() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("errorMessage", "ERROR !!!! ");
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(HTTP_BODY, properties, muleContext);
        MuleMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/resources/error", muleMessage, properties);
        assertEquals("<html><body>ERROR !!!! Internal Server Error</body></html>", response.getPayloadAsString());
        assertEquals("" + HttpConstants.SC_INTERNAL_SERVER_ERROR, response.getInboundProperty("http.status"));
    }

    @Test
    public void testHttpResponseMove() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(HTTP_BODY, muleContext);
        MuleMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/resources/move", muleMessage);
        assertEquals(HTTP_BODY, response.getPayloadAsString());
        assertEquals("" + HttpConstants.SC_MOVED_PERMANENTLY, response.getInboundProperty("http.status"));
        assertEquals("http://localhost:9090/resources/moved", response.<Object>getInboundProperty("Location"));
    }

    @Test
    public void testHttpResponseAll() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(HTTP_BODY, muleContext);
        MuleMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/resources/all", muleMessage);
        assertEquals("Custom body", response.getPayloadAsString());
        assertEquals("" + HttpConstants.SC_NOT_FOUND, response.getInboundProperty("http.status"));
        assertEquals("GET", response.getInboundProperty("Allow"));
        assertEquals("max-age=3600", response.getInboundProperty("Cache-Control"));
        assertEquals("gzip", response.getInboundProperty("Content-Encoding"));
        assertEquals("Thu, 01 Dec 2014 16:00:00 GMT", response.getInboundProperty("Expires"));
        assertEquals("http://localhost:9090", response.getInboundProperty("Location"));
        assertEquals("chunked", response.getInboundProperty("Transfer-Encoding"));
        assertEquals("value1", response.getInboundProperty("header1"));
        Cookie[] cookies = (Cookie[]) response.getInboundProperty("Set-Cookie");
        assertEquals(2, cookies.length);
        validateCookie(cookies[0]);
        validateCookie(cookies[1]);
    }

    @Test
    public void testHttpResponseAllWithExpressions() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map<String, Object> properties = populateProperties();

        DefaultMuleMessage muleMessage = new DefaultMuleMessage(HTTP_BODY, properties, muleContext);
        MuleMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/resources/allExpressions",  muleMessage, properties);
        assertEquals("Custom body", response.getPayloadAsString());
        assertEquals("" + HttpConstants.SC_NOT_FOUND, response.getInboundProperty("http.status"));
        assertEquals("GET", response.getInboundProperty("Allow"));
        assertEquals("max-age=3600", response.getInboundProperty("Cache-Control"));
        assertEquals("gzip", response.getInboundProperty("Content-Encoding"));
        assertEquals("Thu, 01 Dec 2014 16:00:00 GMT", response.getInboundProperty("Expires"));
        assertEquals("http://localhost:9090", response.getInboundProperty("Location"));
        assertEquals("chunked", response.getInboundProperty("Transfer-Encoding"));
        assertEquals("value1", response.getInboundProperty("header1"));
        Cookie[] cookies = (Cookie[]) response.getInboundProperty("Set-Cookie");
        assertEquals(2, cookies.length);
        validateCookie(cookies[0]);
        validateCookie(cookies[1]);
    }

    private Map<String, Object> populateProperties()
    {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("customBody", "Custom body");
        properties.put("version", "1.0");
        properties.put("contentType", "text/html");
        properties.put("status", HttpConstants.SC_NOT_FOUND);
        properties.put("allow", "GET");
        properties.put("cacheControl", "max-age=3600");
        properties.put("contentEncoding", "gzip");
        properties.put("expires", "Thu, 01 Dec 2014 16:00:00 GMT");
        properties.put("location", "http://localhost:9090");
        properties.put("transferEncoding", "chunked");
        properties.put("header1", "header1");
        properties.put("header2", "header2");
        properties.put("value1", "value1");
        properties.put("value2", "value2");
        properties.put("cookie1", "cookie1");
        properties.put("cookie2", "cookie2");
        properties.put("domain", "localhost");
        properties.put("path", "/");
        properties.put("secure", true);
        properties.put("expiryDate", "Fri, 12 Dec 2014 17:00:00 GMT");
        properties.put("maxAge", "1000");
        return properties;

    }

    private void validateCookie(Cookie cookie)
    {
        if("cookie1".equals(cookie.getName()))
        {
            assertEquals("value1", cookie.getValue());
            assertEquals("/", cookie.getPath());
            assertEquals("localhost", cookie.getDomain());
            assertEquals("Fri Dec 12 14:00:00 GMT-03:00 2014", cookie.getExpiryDate().toString());
            assertTrue(cookie.getSecure());
        }
        else
        {
            assertEquals("cookie2", cookie.getName());
            assertEquals("value2", cookie.getValue());
            assertFalse(cookie.getSecure());
        }
    }

}
