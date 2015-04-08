/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.transaction.TransactionConfig;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import java.io.ByteArrayInputStream;
import java.util.TimeZone;

import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MuleServletTestCase extends AbstractMuleTestCase
{
    private TimeZone savedTimeZone;
    private MuleContext muleContext;
    private MuleRegistry registry;

    @Before
    public void setUp()
    {
        savedTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));

        muleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);
        registry = mock(MuleRegistry.class);

        when(muleContext.getRegistry()).thenReturn(registry);
    }

    @After
    public void tearDown()
    {
        TimeZone.setDefault(savedTimeZone);
    }

    @Test
    public void testHttpServletRequest() throws Exception
    {
        String queryParam = "array=1&array=2&param1=param1&noValueParam";
        DefaultMuleEvent event = getTestHttpEvent(queryParam);

        MuleHttpServletRequest request = new MuleHttpServletRequest(event);

        // is payload correct
        ServletInputStream stream = request.getInputStream();
        assertNotNull(stream);
        IOUtils.contentEquals(new ByteArrayInputStream("test".getBytes()), stream);

        // headers
        assertEquals("value", request.getHeader("X-MyHeader"));

        assertEquals("UTF-8", request.getCharacterEncoding());
        assertEquals(-1, request.getContentLength());
        assertEquals("text/plain", request.getContentType());
        assertEquals("/foo", request.getContextPath());
        assertEquals("GET", request.getMethod());
        assertEquals("/bar", request.getPathInfo());
        assertEquals(queryParam, request.getQueryString());
        assertEquals("/foo/bar", request.getRequestURI());
        assertEquals("/foo", request.getServletPath());
        assertEquals("127.0.0.1", request.getServerName());
    }

    @Test
    public void testNoQueryParams() throws Exception
    {
        DefaultMuleEvent event = getTestHttpEvent("");

        MuleHttpServletRequest request = new MuleHttpServletRequest(event);
        assertEquals("", request.getQueryString());
    }

    @Test
    public void testResponse() throws Exception
    {
        DefaultMuleEvent event = getTestHttpEvent("");
        MuleMessage message = event.getMessage();

        MuleHttpServletResponse response = new MuleHttpServletResponse(event);

        response.setStatus(404);
        assertEquals(404, message.getOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));

        // nothing happens with message, the parameter is deprecated
        response.setStatus(200, "status message");
        assertEquals(200, message.getOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));

        response.setContentType("application/octet-stream");
        assertEquals("application/octet-stream",
            message.getOutboundProperty(HttpConstants.HEADER_CONTENT_TYPE));
        assertEquals("application/octet-stream", response.getContentType());

        response.setHeader("X-Test", "value");
        assertTrue(response.containsHeader("X-Test"));
        assertEquals("value", message.getOutboundProperty("X-Test"));

        response.setDateHeader("X-Date", 0);
        assertEquals("Wed, 31 Dec 1969 00:00:00 GMT", message.getOutboundProperty("X-Date"));

        response.sendRedirect("http://anotherplace");
        assertEquals("http://anotherplace", message.getOutboundProperty("Location"));
        assertEquals(302, message.getOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));
    }

    @Test
    public void testCookies() throws Exception
    {
        DefaultMuleEvent event = getTestHttpEvent("");
        MuleMessage message = event.getMessage();

        MuleHttpServletResponse response = new MuleHttpServletResponse(event);

        // ensure cookies work for shiro
        response.addCookie(new Cookie("cookie1", "value"));

        org.apache.commons.httpclient.Cookie[] cookies = message.getOutboundProperty(HttpConnector.HTTP_COOKIES_PROPERTY);
        assertNotNull(cookies);
        org.apache.commons.httpclient.Cookie cookie = cookies[0];
        assertEquals("cookie1", cookie.getName());
        assertEquals("value", cookie.getValue());

        response.addCookie(new Cookie("cookie2", "value"));
        cookies = message.getOutboundProperty(HttpConnector.HTTP_COOKIES_PROPERTY);
        assertNotNull(cookies);
        assertEquals(2, cookies.length);

        cookie = cookies[1];
        assertEquals("cookie2", cookie.getName());
        assertEquals("value", cookie.getValue());
    }

    private DefaultMuleEvent getTestHttpEvent(String queryParam) throws EndpointException
    {
        DefaultMuleMessage message = new DefaultMuleMessage("payload", muleContext);
        message.setInboundProperty("X-MyHeader", "value");
        message.setInboundProperty("Content-Type", "text/plain");
        message.setInboundProperty(HttpConnector.HTTP_METHOD_PROPERTY, "GET");
        message.setInboundProperty(HttpConnector.HTTP_CONTEXT_PATH_PROPERTY, "/foo");
        message.setInboundProperty(HttpConnector.HTTP_REQUEST_PATH_PROPERTY, "/foo/bar");
        if (queryParam != null)
        {
            queryParam = "?" + queryParam;
        }
        message.setInboundProperty(HttpConnector.HTTP_REQUEST_PROPERTY, "/foo/bar" + queryParam);
        message.setInboundProperty(HttpConstants.HEADER_HOST, "127.0.0.1");

        message.setEncoding("UTF-8");

        InboundEndpoint ep = mock(InboundEndpoint.class);
        when(ep.getAddress()).thenReturn("http://localhost:8080/foo");
        when(ep.getName()).thenReturn("test");
        when(ep.getEndpointURI()).thenReturn(new MuleEndpointURI("http://localhost:8080/foo", muleContext));
        TransactionConfig txConfig = mock(TransactionConfig.class);
        when(ep.getTransactionConfig()).thenReturn(txConfig);
        when(ep.getExchangePattern()).thenReturn(MessageExchangePattern.REQUEST_RESPONSE);

        DefaultMuleEvent event = new DefaultMuleEvent(message, ep, (FlowConstruct)null);
        return event;
    }
}
