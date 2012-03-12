/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.transformers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transport.http.CookieWrapper;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.Header;
import org.junit.Test;

public class HttpResponseTransformerTestCase extends AbstractMuleContextTestCase
{
    private static final String HTTP_BODY = "<html><head></head><body><p>This is the response body</p></body></html>";
    private static final String HTTP_BODY_WITH_EXPRESSION = "<html><head></head><body><p>Hello #[header:userName]</p></body></html>";

    @Test
    public void testEmptyHttpResponseTransformer() throws Exception
    {
        HttpResponseTransformer httpResponsetransformer = createHttpResponseTransformer();
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(HTTP_BODY, muleContext);

        HttpResponse httpResponse = (HttpResponse) httpResponsetransformer.transformMessage(muleMessage, "UTF-8");
        assertEquals(HTTP_BODY, httpResponse.getBodyAsString());
        assertEquals(HttpConstants.HTTP11, httpResponse.getHttpVersion().toString());
        assertEquals(HttpConstants.SC_OK, httpResponse.getStatusCode());
        validateHeader(httpResponse.getHeaders(), "Content-Type", HttpConstants.DEFAULT_CONTENT_TYPE);
    }

    @Test
    public void testHttpResponseTransformerAttributes() throws Exception
    {
        HttpResponseTransformer httpResponseTransformer = createHttpResponseTransformer();
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(HTTP_BODY, muleContext);

        httpResponseTransformer.setContentType("text/html");
        httpResponseTransformer.setVersion("1.0");
        httpResponseTransformer.setStatus("500");

        HttpResponse httpResponse = (HttpResponse) httpResponseTransformer.transformMessage(muleMessage, "UTF-8");
        assertEquals(HTTP_BODY, httpResponse.getBodyAsString());
        assertEquals(HttpConstants.HTTP10, httpResponse.getHttpVersion().toString());
        assertEquals(HttpConstants.SC_INTERNAL_SERVER_ERROR, httpResponse.getStatusCode());
        validateHeader(httpResponse.getHeaders(), "Content-Type", "text/html");
    }

    @Test
    public void testHttpResponseTransformerAttributesWithExpressions() throws Exception
    {
        HttpResponseTransformer httpResponseTransformer = createHttpResponseTransformer();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("version", HttpConstants.HTTP10);
        properties.put("status", HttpConstants.SC_INTERNAL_SERVER_ERROR);
        properties.put("contentType", "text/html");

        DefaultMuleMessage muleMessage = new DefaultMuleMessage(HTTP_BODY, properties, muleContext);

        httpResponseTransformer.setVersion("#[header:version]");
        httpResponseTransformer.setStatus("#[header:status]");
        httpResponseTransformer.setContentType("#[header:contentType]");

        HttpResponse httpResponse = (HttpResponse) httpResponseTransformer.transformMessage(muleMessage, "UTF-8");
        assertEquals(HTTP_BODY, httpResponse.getBodyAsString());
        assertEquals(HttpConstants.HTTP10, httpResponse.getHttpVersion().toString());
        assertEquals(HttpConstants.SC_INTERNAL_SERVER_ERROR, httpResponse.getStatusCode());
        validateHeader(httpResponse.getHeaders(), "Content-Type", "text/html");
    }

    @Test
    public void testHttpResponseTransformerHeaders() throws Exception
    {
        HttpResponseTransformer httpResponseTransformer = createHttpResponseTransformer();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("allow", "GET, HEAD");
        properties.put("cacheControl", "max-age=3600");
        properties.put("connection", "close");
        properties.put("contentEncoding", "gzip");
        properties.put("expires", "Thu, 01 Dec 1994 16:00:00 GMT");
        properties.put("location", "http://localhost:8080");
        properties.put("transferEncoding", "chunked");

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Allow", "#[header:allow]");
        headers.put("Cache-Control", "#[header:cacheControl]");
        headers.put("Connection", "#[header:connection]");
        headers.put("Content-Encoding", "#[header:contentEncoding]");
        headers.put("Expires", "#[header:expires]");
        headers.put("Location", "#[header:location]");
        headers.put("Transfer-Encoding", "#[header:transferEncoding]");
        httpResponseTransformer.setHeaders(headers);

        DefaultMuleMessage muleMessage = new DefaultMuleMessage(HTTP_BODY, properties, muleContext);

        HttpResponse httpResponse = (HttpResponse) httpResponseTransformer.transformMessage(muleMessage, "UTF-8");
        validateHeaders(httpResponse.getHeaders());
    }

    @Test
    public void testHttpResponseTransformerHeadersWithExpressions() throws Exception
    {
        HttpResponseTransformer httpResponseTransformer = createHttpResponseTransformer();
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Allow", "GET, HEAD");
        headers.put("Cache-Control", "max-age=3600");
        headers.put("Connection", "close");
        headers.put("Content-Encoding", "gzip");
        headers.put("Expires", "Thu, 01 Dec 1994 16:00:00 GMT");
        headers.put("Location", "http://localhost:8080");
        headers.put("Transfer-Encoding", "chunked");
        httpResponseTransformer.setHeaders(headers);

        DefaultMuleMessage muleMessage = new DefaultMuleMessage(HTTP_BODY, muleContext);

        HttpResponse httpResponse = (HttpResponse) httpResponseTransformer.transformMessage(muleMessage, "UTF-8");
        validateHeaders(httpResponse.getHeaders());
    }

    @Test
    public void testHttpResponseTransformerHeadersWithExpressionInHeaderName() throws Exception
    {
        HttpResponseTransformer httpResponseTransformer = createHttpResponseTransformer();
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("allow", "Allow");

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("#[header:allow]", "GET");
        httpResponseTransformer.setHeaders(headers);

        DefaultMuleMessage muleMessage = new DefaultMuleMessage(HTTP_BODY,  properties, muleContext);

        HttpResponse httpResponse = (HttpResponse) httpResponseTransformer.transformMessage(muleMessage, "UTF-8");
        validateHeader(httpResponse.getHeaders(), "Allow", "GET");
    }

    @Test
    public void testHttpResponseTransformerBodyWithExpression() throws Exception
    {
        HttpResponseTransformer httpResponseTransformer = createHttpResponseTransformer();
        httpResponseTransformer.setBody(HTTP_BODY_WITH_EXPRESSION);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("userName", "John Galt");

        DefaultMuleMessage muleMessage = new DefaultMuleMessage(HTTP_BODY, properties, muleContext);

        HttpResponse httpResponse = (HttpResponse) httpResponseTransformer.transformMessage(muleMessage, "UTF-8");
        assertTrue(httpResponse.getBodyAsString().contains("Hello John Galt"));
    }

    @Test
    public void testHttpResponseTransformerCookies() throws Exception
    {
        HttpResponseTransformer httpResponseTransformer = createHttpResponseTransformer();
        List<CookieWrapper> cookies = new ArrayList<CookieWrapper>();

        cookies.add(createCookie("userName", "John_Galt", "localhost", "/", "Thu, 15 Dec 2013 16:00:00 GMT", "true"));
        cookies.add(createCookie("userId", "1", "localhost", "/", "Thu, 01 Dec 2013 16:00:00 GMT", "true"));

        httpResponseTransformer.setCookies(cookies);

        DefaultMuleMessage muleMessage = new DefaultMuleMessage(HTTP_BODY, muleContext);

        HttpResponse httpResponse = (HttpResponse) httpResponseTransformer.transformMessage(muleMessage, "UTF-8");
        Map<String, String> responseCookies = getHeaderCookie(httpResponse.getHeaders());
        assertNotNull(responseCookies);
        assertEquals("userName=John_Galt; Domain=localhost; Path=/; Secure; Expires=Sun, 15-Dec-2013 16:00:00 GMT", responseCookies.get("userName"));
        assertEquals("userId=1; Domain=localhost; Path=/; Secure; Expires=Sun, 1-Dec-2013 16:00:00 GMT", responseCookies.get("userId"));
    }

    @Test
    public void testHttpResponseTransformerCookiesWithExpressions() throws Exception
    {
        HttpResponseTransformer httpResponseTransformer = createHttpResponseTransformer();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("name", "userName");
        properties.put("value", "John_Galt");
        properties.put("domain", "localhost");
        properties.put("path", "/");
        properties.put("expiryDate", "Thu, 15 Dec 2013 16:00:00 GMT");
        properties.put("secure", "true");

        List<CookieWrapper> cookies = new ArrayList<CookieWrapper>();
        cookies.add(createCookie("#[header:name]", "#[header:value]", "#[header:domain]", "#[header:path]", "#[header:expiryDate]", "#[header:secure]"));
        httpResponseTransformer.setCookies(cookies);

        DefaultMuleMessage muleMessage = new DefaultMuleMessage(HTTP_BODY, properties, muleContext);

        HttpResponse httpResponse = (HttpResponse) httpResponseTransformer.transformMessage(muleMessage, "UTF-8");
        Map<String, String> responseCookies = getHeaderCookie(httpResponse.getHeaders());
        assertNotNull(responseCookies);
        assertEquals("userName=John_Galt; Domain=localhost; Path=/; Secure; Expires=Sun, 15-Dec-2013 16:00:00 GMT", responseCookies.get("userName"));
    }

    private CookieWrapper createCookie(String name, String value, String domain, String path, String expiryDate, String secure)
    {
        CookieWrapper cookie = new CookieWrapper();
        cookie.setName(name);
        cookie.setValue(value);
        cookie.setDomain(domain);
        cookie.setPath(path);
        cookie.setExpiryDate(expiryDate);
        cookie.setSecure(secure);
        return cookie;
    }

    private Map<String, String> getHeaderCookie(Header[] headers)
    {
        Map<String, String> cookies = new HashMap<String, String>();
        for(Header header : headers)
        {
            if("Set-Cookie".equals(header.getName()))
            {
                cookies.put(header.getValue().split("=")[0], header.getValue());
            }
        }
        return cookies;
    }

    private void validateHeaders(Header[] responseHeaders)
    {
        validateHeader(responseHeaders, "Allow", "GET, HEAD");
        validateHeader(responseHeaders, "Cache-Control", "max-age=3600");
        validateHeader(responseHeaders, "Connection", "close");
        validateHeader(responseHeaders, "Content-Encoding", "gzip");
        validateHeader(responseHeaders, "Expires", "Thu, 01 Dec 1994 16:00:00 GMT");
        validateHeader(responseHeaders, "Location", "http://localhost:8080");
        validateHeader(responseHeaders, "Transfer-Encoding", "chunked");
    }

    private HttpResponseTransformer createHttpResponseTransformer()
    {
        HttpResponseTransformer httpResponseTransformer = new HttpResponseTransformer();
        httpResponseTransformer.setMuleContext(muleContext);
        return httpResponseTransformer;
    }

    private void validateHeader(Header[] headers, String headerName, String expectedValue)
    {
        for(Header header : headers)
        {
            if(headerName.equals(header.getName()))
            {
                assertEquals(expectedValue, header.getValue());
            }
        }
    }


}
