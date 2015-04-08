/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transport.http.CacheControlHeader;
import org.mule.transport.http.CookieHelper;
import org.mule.transport.http.CookieWrapper;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpResponse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@SmallTest
public class HttpResponseBuilderTestCase extends AbstractMuleTestCase
{
    private static final String HTTP_BODY = "<html><head></head><body><p>This is the response body</p></body></html>";
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
    private static final String HEADER_DIRECTIVE = "#[header:directive]";
    private static final String HEADER_MAX_AGE = "#[header:maxAge]";
    private static final String HEADER_MUST_REVALIDATE = "#[header:mustRevalidate]";
    private static final String HEADER_NO_CACHE = "#[header:noCache]";
    private static final String HEADER_NO_STORE = "#[header:noStore]";


    private MuleContext muleContext;
    private MuleMessage mockMuleMessage;
    private ExpressionManager mockExpressionManager = Mockito.mock(ExpressionManager.class);
    private MuleEvent mockEvent;

    @Before
    public void setUp()
    {
        muleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);
        mockEvent = mock(MuleEvent.class);
        mockMuleMessage = mock(MuleMessage.class);
        mockExpressionManager = mock(ExpressionManager.class);
        when(muleContext.getExpressionManager()).thenReturn(mockExpressionManager);
    }

    @Test
    public void testEmptyHttpResponseBuilder() throws Exception
    {
        HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(HTTP_BODY, muleContext);

        when(mockEvent.getMessage()).thenReturn(muleMessage);

        mockParse();
        HttpResponse httpResponse = (HttpResponse) httpResponseBuilder.process(mockEvent).getMessage().getPayload();
        assertEquals(HTTP_BODY, httpResponse.getBodyAsString());
        assertEquals(HttpConstants.HTTP11, httpResponse.getHttpVersion().toString());
        assertEquals(HttpConstants.SC_OK, httpResponse.getStatusCode());
        validateHeader(httpResponse.getHeaders(), HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.DEFAULT_CONTENT_TYPE);
    }

    @Test
    public void testHttpResponseBuilderAttributes() throws Exception
    {
        HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(HTTP_BODY, muleContext);
        when(mockEvent.getMessage()).thenReturn(muleMessage);

        httpResponseBuilder.setContentType("text/html");
        httpResponseBuilder.setStatus(String.valueOf(HttpConstants.SC_INTERNAL_SERVER_ERROR));

        mockParse();
        HttpResponse httpResponse = (HttpResponse) httpResponseBuilder.process(mockEvent).getMessage().getPayload();
        assertEquals(HTTP_BODY, httpResponse.getBodyAsString());
        assertEquals(HttpConstants.HTTP11, httpResponse.getHttpVersion().toString());
        assertEquals(HttpConstants.SC_INTERNAL_SERVER_ERROR, httpResponse.getStatusCode());
        validateHeader(httpResponse.getHeaders(), HttpConstants.HEADER_CONTENT_TYPE, "text/html");
    }

    @Test
    public void testHttpResponseBuilderAttributesWithExpressions() throws Exception
    {
        HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(HTTP_BODY, muleContext);
        when(mockEvent.getMessage()).thenReturn(muleMessage);

        httpResponseBuilder.setStatus(HEADER_STATUS);
        httpResponseBuilder.setContentType(HEADER_CONTENT_TYPE);

        when(mockExpressionManager.parse(HEADER_STATUS, muleMessage)).thenReturn(String.valueOf(HttpConstants.SC_INTERNAL_SERVER_ERROR));
        when(mockExpressionManager.parse(HEADER_CONTENT_TYPE, muleMessage)).thenReturn("text/html");


        HttpResponse httpResponse = (HttpResponse) httpResponseBuilder.process(mockEvent).getMessage().getPayload();
        assertEquals(HTTP_BODY, httpResponse.getBodyAsString());
        assertEquals(HttpConstants.HTTP11, httpResponse.getHttpVersion().toString());
        assertEquals(HttpConstants.SC_INTERNAL_SERVER_ERROR, httpResponse.getStatusCode());
        validateHeader(httpResponse.getHeaders(), HttpConstants.HEADER_CONTENT_TYPE, "text/html");
    }

    @Test
    public void testHttpResponseBuilderHeadersWithExpressions() throws Exception
    {
        HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Cache-Control", HEADER_CACHE_CONTROL);
        headers.put("Expires", HEADER_EXPIRES);
        headers.put("Location", HEADER_LOCATION);
        httpResponseBuilder.setHeaders(headers);

        when(mockExpressionManager.parse("Cache-Control", mockMuleMessage)).thenReturn("Cache-Control");
        when(mockExpressionManager.parse("Expires", mockMuleMessage)).thenReturn("Expires");
        when(mockExpressionManager.parse("Location", mockMuleMessage)).thenReturn("Location");
        when(mockExpressionManager.parse(HEADER_CACHE_CONTROL, mockMuleMessage)).thenReturn("max-age=3600");
        when(mockExpressionManager.isExpression(HEADER_EXPIRES)).thenReturn(true);
        when(mockExpressionManager.evaluate(HEADER_EXPIRES, mockMuleMessage)).thenReturn("Thu, 01 Dec 1994 16:00:00 GMT");
        when(mockExpressionManager.parse(HEADER_LOCATION, mockMuleMessage)).thenReturn("http://localhost:8080");

        HttpResponse response = new HttpResponse();
        httpResponseBuilder.setHeaders(response, mockMuleMessage);

        validateHeaders(response.getHeaders());
    }

    @Test
    public void testHttpResponseBuilderHeaders() throws Exception
    {
        HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Cache-Control", "max-age=3600");
        headers.put("Expires", "Thu, 01 Dec 1994 16:00:00 GMT");
        headers.put("Location", "http://localhost:8080");
        httpResponseBuilder.setHeaders(headers);

        mockParse();
        HttpResponse response = new HttpResponse();
        httpResponseBuilder.setHeaders(response, mockMuleMessage);

        validateHeaders(response.getHeaders());
    }

    @Test
    public void testHttpResponseBuilderHeadersWithExpressionInHeaderName() throws Exception
    {
        HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();

        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HEADER_LOCATION, "http://localhost:9090");
        httpResponseBuilder.setHeaders(headers);

        when(mockExpressionManager.parse(HEADER_LOCATION, mockMuleMessage)).thenReturn("Location");
        when(mockExpressionManager.parse("http://localhost:9090", mockMuleMessage)).thenReturn("http://localhost:9090");

        HttpResponse response = new HttpResponse();
        httpResponseBuilder.setHeaders(response, mockMuleMessage);

        validateHeader(response.getHeaders(), "Location", "http://localhost:9090");
    }

    @Test
    public void testHttpResponseBuilderCookies() throws Exception
    {
        HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();
        List<CookieWrapper> cookies = new ArrayList<CookieWrapper>();

        cookies.add(createCookie("userName", "John_Galt", "localhost", "/", "Thu, 15 Dec 2013 16:00:00 GMT", "true", "1"));
        cookies.add(createCookie("userId", "1", "localhost", "/", "Thu, 01 Dec 2013 16:00:00 GMT", "true", "1"));

        mockParse();

        httpResponseBuilder.setCookies(cookies);

        HttpResponse response = new HttpResponse();
        httpResponseBuilder.setCookies(response, mockMuleMessage);

        Map<String, String> responseCookies = getHeaderCookie(response.getHeaders());
        assertNotNull(responseCookies);
        assertEquals("userName=John_Galt; Version=1; Domain=localhost; Path=/; Secure; Expires=Sun, 15-Dec-2013 16:00:00 GMT", responseCookies.get("userName"));
        assertEquals("userId=1; Version=1; Domain=localhost; Path=/; Secure; Expires=Sun, 1-Dec-2013 16:00:00 GMT", responseCookies.get("userId"));
    }

    @Test
    public void testHttpResponseBuilderCookiesWithExpressions() throws Exception
    {
        HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();

        List<CookieWrapper> cookies = new ArrayList<CookieWrapper>();
        cookies.add(createCookie(HEADER_NAME, HEADER_VALUE, HEADER_DOMAIN, HEADER_PATH, HEADER_EXPIRY_DATE, HEADER_SECURE, HEADER_VERSION));
        httpResponseBuilder.setCookies(cookies);

        when(mockExpressionManager.parse(HEADER_NAME, mockMuleMessage)).thenReturn("userName");
        when(mockExpressionManager.parse(HEADER_VALUE, mockMuleMessage)).thenReturn("John_Galt");
        when(mockExpressionManager.parse(HEADER_DOMAIN, mockMuleMessage)).thenReturn("localhost");
        when(mockExpressionManager.parse(HEADER_PATH, mockMuleMessage)).thenReturn("/");
        when(mockExpressionManager.isExpression(HEADER_EXPIRY_DATE)).thenReturn(true);
        when(mockExpressionManager.evaluate(HEADER_EXPIRY_DATE, mockMuleMessage)).thenReturn("Sun, 15 Dec 2013 16:00:00 GMT");
        when(mockExpressionManager.parse(HEADER_SECURE, mockMuleMessage)).thenReturn("true");
        when(mockExpressionManager.parse(HEADER_VERSION, mockMuleMessage)).thenReturn("1");

        HttpResponse response = new HttpResponse();
        httpResponseBuilder.setCookies(response, mockMuleMessage);

        Map<String, String> responseCookies = getHeaderCookie(response.getHeaders());
        assertNotNull(responseCookies);
        assertEquals("userName=John_Galt; Version=1; Domain=localhost; Path=/; Secure; Expires=Sun, 15-Dec-2013 16:00:00 GMT", responseCookies.get("userName"));
    }

    @Test
    public void testHttpResponseDefaultVersion() throws Exception
    {
        HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();
        when(mockMuleMessage.getInboundProperty(HttpConnector.HTTP_VERSION_PROPERTY)).thenReturn(HttpConstants.HTTP10);

        httpResponseBuilder.checkVersion(mockMuleMessage);

        assertEquals(HttpConstants.HTTP10, httpResponseBuilder.getVersion());
    }

    @Test
    public void testHttpResponseDefaultContentType() throws Exception
    {
        HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();
        when(mockMuleMessage.getInboundProperty(HttpConstants.HEADER_CONTENT_TYPE)).thenReturn("text/html");

        HttpResponse response = new HttpResponse();
        mockParse();
        httpResponseBuilder.setContentType(response, mockMuleMessage);

        validateHeader(response.getHeaders(), HttpConstants.HEADER_CONTENT_TYPE, "text/html");
    }

    @Test
    public void testHttpResponseEmptyCacheControl() throws Exception
    {
        HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();
        httpResponseBuilder.setCacheControl(new CacheControlHeader());

        HttpResponse response = new HttpResponse();
        httpResponseBuilder.setCacheControl(response, mockMuleMessage);
        assertNull(response.getFirstHeader(HttpConstants.HEADER_CACHE_CONTROL));
    }

    @Test
    public void testHttpResponseCacheControl() throws Exception
    {
        HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();
        CacheControlHeader cacheControl = new CacheControlHeader();
        cacheControl.setDirective("public");
        cacheControl.setMaxAge("3600");
        cacheControl.setMustRevalidate("true");
        cacheControl.setNoCache("true");
        cacheControl.setNoStore("true");
        httpResponseBuilder.setCacheControl(cacheControl);
        mockParse();

        HttpResponse response = new HttpResponse();
        httpResponseBuilder.setCacheControl(response, mockMuleMessage);
        assertEquals("public,no-cache,no-store,must-revalidate,max-age=3600", response.getFirstHeader(HttpConstants.HEADER_CACHE_CONTROL).getValue());
    }

    @Test
    public void testHttpResponseCacheControlWithExpressions() throws Exception
    {
        HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();
        CacheControlHeader cacheControl = new CacheControlHeader();
        cacheControl.setDirective(HEADER_DIRECTIVE);
        cacheControl.setMaxAge(HEADER_MAX_AGE);
        cacheControl.setMustRevalidate(HEADER_MUST_REVALIDATE);
        cacheControl.setNoCache(HEADER_NO_CACHE);
        cacheControl.setNoStore(HEADER_NO_STORE);
        httpResponseBuilder.setCacheControl(cacheControl);

        when(mockExpressionManager.parse(HEADER_DIRECTIVE, mockMuleMessage)).thenReturn("public");
        when(mockExpressionManager.parse(HEADER_MAX_AGE, mockMuleMessage)).thenReturn("3600");
        when(mockExpressionManager.parse(HEADER_MUST_REVALIDATE, mockMuleMessage)).thenReturn("true");
        when(mockExpressionManager.parse(HEADER_NO_CACHE, mockMuleMessage)).thenReturn("true");
        when(mockExpressionManager.parse(HEADER_NO_STORE, mockMuleMessage)).thenReturn("true");

        HttpResponse response = new HttpResponse();
        httpResponseBuilder.setCacheControl(response, mockMuleMessage);
        assertEquals("public,no-cache,no-store,must-revalidate,max-age=3600", response.getFirstHeader(HttpConstants.HEADER_CACHE_CONTROL).getValue());
    }

    @Test
    public void testHttpResponseCacheControlWithExtension() throws Exception
    {
        HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();
        CacheControlHeader cacheControl = new CacheControlHeader();
        cacheControl.setMaxAge("3600");
        httpResponseBuilder.setCacheControl(cacheControl);
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpConstants.HEADER_CACHE_CONTROL, "smax-age=3600");
        httpResponseBuilder.setHeaders(headers);
        mockParse();

        HttpResponse response = new HttpResponse();
        httpResponseBuilder.setHeaders(response, mockMuleMessage);
        httpResponseBuilder.setCacheControl(response, mockMuleMessage);

        assertEquals("max-age=3600,smax-age=3600", response.getFirstHeader(HttpConstants.HEADER_CACHE_CONTROL).getValue());

    }

    @Test
    public void testHttpResponseCopyOutboundProperties() throws Exception
    {
        HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();
        Map<String, Object> outboundProperties = new HashMap<String, Object>();
        outboundProperties.put(HttpConstants.HEADER_AGE, "12");
        outboundProperties.put(HttpConstants.HEADER_CACHE_CONTROL, "max-age=3600");
        outboundProperties.put(MuleProperties.MULE_ENCODING_PROPERTY, "UTF-8");
        Cookie[] cookies = new Cookie[2];
        cookies[0] = new Cookie(null, "clientId", "2");
        cookies[1] = new Cookie(null, "category", "premium");
        outboundProperties.put(HttpConstants.HEADER_COOKIE_SET, cookies);

        Set<String> propertyNames =  outboundProperties.keySet();
        when(mockMuleMessage.getOutboundPropertyNames()).thenReturn(propertyNames);
        for(String propertyName : propertyNames)
        {
            when(mockMuleMessage.getOutboundProperty(propertyName)).thenReturn(outboundProperties.get(propertyName));
        }

        HttpResponse response = new HttpResponse();
        httpResponseBuilder.copyOutboundProperties(response, mockMuleMessage);

        Header[] headers = response.getHeaders();
        for(Header header : headers)
        {
            if(HttpConstants.HEADER_COOKIE_SET.equals(header.getName()))
            {
                if(header.getValue().startsWith(cookies[0].getName()))
                {
                    assertEquals(cookies[0].toString(), header.getValue());
                }
                else
                {
                    assertEquals(cookies[1].toString(), header.getValue());
                }

            }
            else if(header.getName().startsWith(HttpConstants.CUSTOM_HEADER_PREFIX))
            {
                assertEquals(outboundProperties.get(header.getName().substring(HttpConstants.CUSTOM_HEADER_PREFIX.length())), header.getValue());
            }
            else
            {
                assertEquals(outboundProperties.get(header.getName()), header.getValue());
            }
        }
    }

    @Test
    public void testHttpResponseWithOutboundProperties() throws Exception
    {
        HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();
        CacheControlHeader cacheControl = new CacheControlHeader();
        cacheControl.setMaxAge("3600");
        httpResponseBuilder.setCacheControl(cacheControl);

        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpConstants.HEADER_CACHE_CONTROL, "public");
        headers.put(HttpConstants.HEADER_AGE, "12");
        httpResponseBuilder.setHeaders(headers);

        Map<String, Object> outboundProperties = new HashMap<String, Object>();
        outboundProperties.put(HttpConstants.HEADER_CACHE_CONTROL, "no-cache");
        outboundProperties.put(HttpConstants.HEADER_AGE, "20");
        outboundProperties.put(HttpConstants.HEADER_LOCATION, "http://localhost:9090");

        mockParse();
        DefaultMuleMessage message = new DefaultMuleMessage(HTTP_BODY, outboundProperties, muleContext);

        when(mockEvent.getMessage()).thenReturn(message);
        HttpResponse httpResponse = (HttpResponse) httpResponseBuilder.process(mockEvent).getMessage().getPayload();
        Header[] resultHeaders = httpResponse.getHeaders();
        validateHeader(resultHeaders, HttpConstants.HEADER_CACHE_CONTROL, "max-age=3600,public");
        validateHeader(resultHeaders, HttpConstants.HEADER_AGE, "12");
        validateHeader(resultHeaders, HttpConstants.HEADER_LOCATION, "http://localhost:9090");
    }

    @Test
    public void testHttpResponseWithDateExpression() throws Exception
    {
        HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpConstants.HEADER_EXPIRES, "#[now]");
        httpResponseBuilder.setHeaders(headers);

        Date now = new Date();

        when(mockExpressionManager.parse("Expires", mockMuleMessage)).thenReturn("Expires");
        when(mockExpressionManager.isExpression("#[now]")).thenReturn(true);
        when(mockExpressionManager.evaluate("#[now]", mockMuleMessage)).thenReturn(now);

        HttpResponse httpResponse = new HttpResponse();
        httpResponseBuilder.setHeaders(httpResponse, mockMuleMessage);

        SimpleDateFormat httpDateFormatter = new SimpleDateFormat(HttpConstants.DATE_FORMAT_RFC822, Locale.US);
        httpDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));

        validateHeader(httpResponse.getHeaders(), HttpConstants.HEADER_EXPIRES, httpDateFormatter.format(now));
    }


    @Test
    public void testHttpResponseCookieWithDateBuilder() throws Exception
    {
        HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();
        Date now = new Date();
        List<CookieWrapper> cookies = new ArrayList<CookieWrapper>();
        cookies.add(createCookie("test", "test", null, null, "#[now]", null, null));
        httpResponseBuilder.setCookies(cookies);

        when(mockExpressionManager.isExpression("#[now]")).thenReturn(true);
        when(mockExpressionManager.evaluate("#[now]", mockMuleMessage)).thenReturn(now);
        when(mockExpressionManager.parse("test", mockMuleMessage)).thenReturn("test");
        when(mockExpressionManager.parse("test", mockMuleMessage)).thenReturn("test");

        HttpResponse response = new HttpResponse();
        httpResponseBuilder.setCookies(response, mockMuleMessage);

        SimpleDateFormat httpCookieFormatter = new SimpleDateFormat(CookieHelper.EXPIRE_PATTERN, Locale.US);
        httpCookieFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));

        String expectedCookieValue = "test=test; Expires=" + httpCookieFormatter.format(now);
        validateHeader(response.getHeaders(), HttpConstants.HEADER_COOKIE_SET, expectedCookieValue);
    }

    @Test
    public void testHttpResponseSetBodyWithHttpResponsePayload() throws Exception
    {
        HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();
        HttpResponse response = new HttpResponse();
        response.setBody(HTTP_BODY);

        when(mockMuleMessage.getPayload()).thenReturn(response);

        httpResponseBuilder.setBody(response, mockMuleMessage, mockEvent);
        assertEquals(HTTP_BODY, response.getBodyAsString());
    }

    @Test
    public void testHttpResponseSetBody() throws Exception
    {
        HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();
        HttpResponse response = new HttpResponse();

        when(mockMuleMessage.getPayload()).thenReturn(HTTP_BODY);

        httpResponseBuilder.setBody(response, mockMuleMessage, mockEvent);
        assertEquals(HTTP_BODY, response.getBodyAsString());
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

    private HttpResponseBuilder createHttpResponseBuilder() throws InitialisationException
    {
        HttpResponseBuilder httpResponseBuilder = new HttpResponseBuilder();
        httpResponseBuilder.setMuleContext(muleContext);
        httpResponseBuilder.initialise();
        return httpResponseBuilder;
    }

    private void validateHeader(Header[] headers, String headerName, String expectedValue)
    {
        for(Header header : headers)
        {
            if(headerName.equals(header.getName()))
            {
                assertEquals(expectedValue, header.getValue());
                return;
            }
        }
        fail(String.format("Didn't find header: %s=%s", headerName, expectedValue));
    }

    private void mockParse()
    {
         when(mockExpressionManager.parse(anyString(), Mockito.any(MuleMessage.class))).thenAnswer(
                 new Answer<Object>()
                 {
                     @Override
                     public Object answer(InvocationOnMock invocation) throws Throwable
                     {
                         return invocation.getArguments()[0];
                     }
                 }
         );
    }
}
