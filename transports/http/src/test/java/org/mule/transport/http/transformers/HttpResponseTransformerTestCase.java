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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.expression.ExpressionManager;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transport.http.CookieWrapper;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.Header;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

@SmallTest
public class HttpResponseTransformerTestCase extends AbstractMuleTestCase
{
    private static final String HTTP_BODY = "<html><head></head><body><p>This is the response body</p></body></html>";
    private static final String HTTP_BODY_WITH_EXPRESSION = "<html><head></head><body><p>Hello #[header:userName]</p></body></html>";
    private static final String HEADER_STATUS = "#[header:status]";
    private static final String HEADER_CONTENT_TYPE = "#[header:contentType]";
    private static final String HEADER_CACHE_CONTROL = "#[header:cacheControl]";
    private static final String HEADER_EXPIRES = "#[header:expires]";
    private static final String HEADER_LOCATION = "#[header:location]";
    private static final String HEADER_NAME = "#[header:name]";
    private static final String HEADER_VALUE = "#[header:value]";
    private static final String HEADER_DOMAIN = "#[header:domain]";
    private static final String HEADER_PATH = "#[header:path]";
    private static final String HEADER_EXPIRY_DATE = "#[header:expiryDate]";
    private static final String HEADER_SECURE = "#[header:secure]";
    private static final String HEADER_VERSION = "#[header:version]";


    private MuleContext muleContext;
    private ExpressionManager mockExpressionManager = Mockito.mock(ExpressionManager.class);

    @Before
    public void setUp()
    {
        muleContext = mock(MuleContext.class);
        mockExpressionManager = mock(ExpressionManager.class);
        when(muleContext.getExpressionManager()).thenReturn(mockExpressionManager);
    }


    @Test
    public void testEmptyHttpResponseTransformer() throws Exception
    {
        HttpResponseTransformer httpResponsetransformer = createHttpResponseTransformer();
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(HTTP_BODY, muleContext);

        when(mockExpressionManager.isExpression(Mockito.anyString())).thenReturn(false);

        HttpResponse httpResponse = (HttpResponse) httpResponsetransformer.transformMessage(muleMessage, "UTF-8");
        assertEquals(HTTP_BODY, httpResponse.getBodyAsString());
        assertEquals(HttpConstants.HTTP11, httpResponse.getHttpVersion().toString());
        assertEquals(HttpConstants.SC_OK, httpResponse.getStatusCode());
        validateHeader(httpResponse.getHeaders(), HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.DEFAULT_CONTENT_TYPE);
    }

    @Test
    public void testHttpResponseTransformerAttributes() throws Exception
    {
        HttpResponseTransformer httpResponseTransformer = createHttpResponseTransformer();
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(HTTP_BODY, muleContext);

        when(mockExpressionManager.isExpression(Mockito.anyString())).thenReturn(false);

        httpResponseTransformer.setContentType("text/html");
        httpResponseTransformer.setStatus(String.valueOf(HttpConstants.SC_INTERNAL_SERVER_ERROR));

        HttpResponse httpResponse = (HttpResponse) httpResponseTransformer.transformMessage(muleMessage, "UTF-8");
        assertEquals(HTTP_BODY, httpResponse.getBodyAsString());
        assertEquals(HttpConstants.HTTP11, httpResponse.getHttpVersion().toString());
        assertEquals(HttpConstants.SC_INTERNAL_SERVER_ERROR, httpResponse.getStatusCode());
        validateHeader(httpResponse.getHeaders(), HttpConstants.HEADER_CONTENT_TYPE, "text/html");
    }

    @Test
    public void testHttpResponseTransformerAttributesWithExpressions() throws Exception
    {
        HttpResponseTransformer httpResponseTransformer = createHttpResponseTransformer();
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(HTTP_BODY, muleContext);

        httpResponseTransformer.setStatus(HEADER_STATUS);
        httpResponseTransformer.setContentType(HEADER_CONTENT_TYPE);

        when(mockExpressionManager.isExpression(HEADER_STATUS)).thenReturn(true);
        when(mockExpressionManager.evaluate(HEADER_STATUS, muleMessage)).thenReturn(HttpConstants.SC_INTERNAL_SERVER_ERROR);
        when(mockExpressionManager.isExpression(HEADER_CONTENT_TYPE)).thenReturn(true);
        when(mockExpressionManager.evaluate(HEADER_CONTENT_TYPE, muleMessage)).thenReturn("text/html");


        HttpResponse httpResponse = (HttpResponse) httpResponseTransformer.transformMessage(muleMessage, "UTF-8");
        assertEquals(HTTP_BODY, httpResponse.getBodyAsString());
        assertEquals(HttpConstants.HTTP11, httpResponse.getHttpVersion().toString());
        assertEquals(HttpConstants.SC_INTERNAL_SERVER_ERROR, httpResponse.getStatusCode());
        validateHeader(httpResponse.getHeaders(), HttpConstants.HEADER_CONTENT_TYPE, "text/html");
    }

    @Test
    public void testHttpResponseTransformerHeaders() throws Exception
    {
        HttpResponseTransformer httpResponseTransformer = createHttpResponseTransformer();
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(HTTP_BODY, muleContext);

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Cache-Control", HEADER_CACHE_CONTROL);
        headers.put("Expires", HEADER_EXPIRES);
        headers.put("Location", HEADER_LOCATION);
        httpResponseTransformer.setHeaders(headers);

        when(mockExpressionManager.isExpression(HEADER_CACHE_CONTROL)).thenReturn(true);
        when(mockExpressionManager.evaluate(HEADER_CACHE_CONTROL, muleMessage)).thenReturn("max-age=3600");
        when(mockExpressionManager.isExpression(HEADER_EXPIRES)).thenReturn(true);
        when(mockExpressionManager.evaluate(HEADER_EXPIRES, muleMessage)).thenReturn("Thu, 01 Dec 1994 16:00:00 GMT");
        when(mockExpressionManager.isExpression(HEADER_LOCATION)).thenReturn(true);
        when(mockExpressionManager.evaluate(HEADER_LOCATION, muleMessage)).thenReturn("http://localhost:8080");


        HttpResponse httpResponse = (HttpResponse) httpResponseTransformer.transformMessage(muleMessage, "UTF-8");
        validateHeaders(httpResponse.getHeaders());
    }

    @Test
    public void testHttpResponseTransformerHeadersWithExpressions() throws Exception
    {
        HttpResponseTransformer httpResponseTransformer = createHttpResponseTransformer();
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Cache-Control", "max-age=3600");
        headers.put("Expires", "Thu, 01 Dec 1994 16:00:00 GMT");
        headers.put("Location", "http://localhost:8080");
        httpResponseTransformer.setHeaders(headers);

        DefaultMuleMessage muleMessage = new DefaultMuleMessage(HTTP_BODY, muleContext);

        HttpResponse httpResponse = (HttpResponse) httpResponseTransformer.transformMessage(muleMessage, "UTF-8");
        validateHeaders(httpResponse.getHeaders());
    }

    @Test
    public void testHttpResponseTransformerHeadersWithExpressionInHeaderName() throws Exception
    {
        HttpResponseTransformer httpResponseTransformer = createHttpResponseTransformer();

        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HEADER_LOCATION, "http://localhost:9090");
        httpResponseTransformer.setHeaders(headers);

        DefaultMuleMessage muleMessage = new DefaultMuleMessage(HTTP_BODY, muleContext);

        when(mockExpressionManager.isExpression(HEADER_LOCATION)).thenReturn(true);
        when(mockExpressionManager.evaluate(HEADER_LOCATION, muleMessage)).thenReturn("http://localhost:9090");


        HttpResponse httpResponse = (HttpResponse) httpResponseTransformer.transformMessage(muleMessage, "UTF-8");
        validateHeader(httpResponse.getHeaders(), "Location", "http://localhost:9090");
    }

    @Test
    public void testHttpResponseTransformerBodyWithExpression() throws Exception
    {
        HttpResponseTransformer httpResponseTransformer = createHttpResponseTransformer();
        httpResponseTransformer.setBody(HTTP_BODY_WITH_EXPRESSION);

        DefaultMuleMessage muleMessage = new DefaultMuleMessage(HTTP_BODY, muleContext);
        when(mockExpressionManager.parse(HTTP_BODY_WITH_EXPRESSION, muleMessage)).thenReturn("<html><head></head><body><p>Hello John Galt</p></body></html>");


        HttpResponse httpResponse = (HttpResponse) httpResponseTransformer.transformMessage(muleMessage, "UTF-8");
        assertTrue(httpResponse.getBodyAsString().contains("Hello John Galt"));
    }

    @Test
    public void testHttpResponseTransformerCookies() throws Exception
    {
        HttpResponseTransformer httpResponseTransformer = createHttpResponseTransformer();
        List<CookieWrapper> cookies = new ArrayList<CookieWrapper>();

        cookies.add(createCookie("userName", "John_Galt", "localhost", "/", "Thu, 15 Dec 2013 16:00:00 GMT", "true", "1"));
        cookies.add(createCookie("userId", "1", "localhost", "/", "Thu, 01 Dec 2013 16:00:00 GMT", "true", "1"));

        httpResponseTransformer.setCookies(cookies);

        DefaultMuleMessage muleMessage = new DefaultMuleMessage(HTTP_BODY, muleContext);

        HttpResponse httpResponse = (HttpResponse) httpResponseTransformer.transformMessage(muleMessage, "UTF-8");
        Map<String, String> responseCookies = getHeaderCookie(httpResponse.getHeaders());
        assertNotNull(responseCookies);
        assertEquals("userName=John_Galt; Version=1; Domain=localhost; Path=/; Secure; Expires=Sun, 15-Dec-2013 16:00:00 GMT", responseCookies.get("userName"));
        assertEquals("userId=1; Version=1; Domain=localhost; Path=/; Secure; Expires=Sun, 1-Dec-2013 16:00:00 GMT", responseCookies.get("userId"));
    }

    @Test
    public void testHttpResponseTransformerCookiesWithExpressions() throws Exception
    {
        HttpResponseTransformer httpResponseTransformer = createHttpResponseTransformer();

        List<CookieWrapper> cookies = new ArrayList<CookieWrapper>();
        cookies.add(createCookie(HEADER_NAME, HEADER_VALUE, HEADER_DOMAIN, HEADER_PATH, HEADER_EXPIRY_DATE, HEADER_SECURE, HEADER_VERSION));
        httpResponseTransformer.setCookies(cookies);

        DefaultMuleMessage muleMessage = new DefaultMuleMessage(HTTP_BODY, muleContext);

        when(mockExpressionManager.isExpression(HEADER_NAME)).thenReturn(true);
        when(mockExpressionManager.evaluate(HEADER_NAME, muleMessage)).thenReturn("userName");
        when(mockExpressionManager.isExpression(HEADER_VALUE)).thenReturn(true);
        when(mockExpressionManager.evaluate(HEADER_VALUE, muleMessage)).thenReturn("John_Galt");
        when(mockExpressionManager.isExpression(HEADER_DOMAIN)).thenReturn(true);
        when(mockExpressionManager.evaluate(HEADER_DOMAIN, muleMessage)).thenReturn("localhost");
        when(mockExpressionManager.isExpression(HEADER_PATH)).thenReturn(true);
        when(mockExpressionManager.evaluate(HEADER_PATH, muleMessage)).thenReturn("/");
        when(mockExpressionManager.isExpression(HEADER_EXPIRY_DATE)).thenReturn(true);
        when(mockExpressionManager.evaluate(HEADER_EXPIRY_DATE, muleMessage)).thenReturn("Thu, 15 Dec 2013 16:00:00 GMT");
        when(mockExpressionManager.isExpression(HEADER_SECURE)).thenReturn(true);
        when(mockExpressionManager.evaluate(HEADER_SECURE, muleMessage)).thenReturn("true");
        when(mockExpressionManager.isExpression(HEADER_VERSION)).thenReturn(true);
        when(mockExpressionManager.evaluate(HEADER_VERSION, muleMessage)).thenReturn("1");

        HttpResponse httpResponse = (HttpResponse) httpResponseTransformer.transformMessage(muleMessage, "UTF-8");
        Map<String, String> responseCookies = getHeaderCookie(httpResponse.getHeaders());
        assertNotNull(responseCookies);
        assertEquals("userName=John_Galt; Version=1; Domain=localhost; Path=/; Secure; Expires=Sun, 15-Dec-2013 16:00:00 GMT", responseCookies.get("userName"));
    }

    @Test
    public void testHttpResponseDefaultVersion() throws Exception
    {
        HttpResponseTransformer httpResponseTransformer = createHttpResponseTransformer();
        DefaultMuleMessage message = new DefaultMuleMessage(HTTP_BODY, muleContext);
        message.setInboundProperty(HttpConnector.HTTP_VERSION_PROPERTY, HttpConstants.HTTP10);

        HttpResponse httpResponse = (HttpResponse) httpResponseTransformer.transformMessage(message, "UTF-8");

        assertEquals(HttpConstants.HTTP10, httpResponse.getHttpVersion().toString());
    }

    @Test
    public void testHttpResponseDefaultContentType() throws Exception
    {
        HttpResponseTransformer httpResponseTransformer = createHttpResponseTransformer();
        DefaultMuleMessage message = new DefaultMuleMessage(HTTP_BODY, muleContext);
        message.setInboundProperty(HttpConstants.HEADER_CONTENT_TYPE, "text/html");

        HttpResponse httpResponse = (HttpResponse) httpResponseTransformer.transformMessage(message, "UTF-8");

        validateHeader(httpResponse.getHeaders(), HttpConstants.HEADER_CONTENT_TYPE, "text/html");
    }


    private CookieWrapper createCookie(String name, String value, String domain, String path, String expiryDate, String secure, String version)
    {
        CookieWrapper cookie = new CookieWrapper();
        cookie.setName(name);
        cookie.setValue(value);
        cookie.setDomain(domain);
        cookie.setPath(path);
        cookie.setExpiryDate(expiryDate);
        cookie.setSecure(secure);
        cookie.setVersion(version);
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
        validateHeader(responseHeaders, "Cache-Control", "max-age=3600");
        validateHeader(responseHeaders, "Expires", "Thu, 01 Dec 1994 16:00:00 GMT");
        validateHeader(responseHeaders, "Location", "http://localhost:8080");
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
