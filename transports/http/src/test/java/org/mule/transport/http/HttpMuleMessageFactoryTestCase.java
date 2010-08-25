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

import org.mule.MessageExchangePattern;
import org.mule.api.MuleMessage;
import org.mule.api.transport.MessageTypeNotSupportedException;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.transport.AbstractMuleMessageFactoryTestCase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.StatusLine;
import org.apache.commons.httpclient.URI;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpMuleMessageFactoryTestCase extends AbstractMuleMessageFactoryTestCase
{
    private static final Header[] HEADERS = new Header[] { new Header("foo-header", "foo-value") };
    private static final String REQUEST_LINE = "GET /services/Echo HTTP/1.1";

    @Override
    protected MuleMessageFactory doCreateMuleMessageFactory()
    {
        return new HttpMuleMessageFactory(muleContext);
    }

    @Override
    protected Object getValidTransportMessage() throws Exception
    {
        RequestLine requestLine = RequestLine.parseLine(REQUEST_LINE);
        HttpRequest request = new HttpRequest(requestLine, HEADERS, null, encoding);
        return request;
    }
    
    @Override
    protected Object getUnsupportedTransportMessage()
    {
        return "this is not a valid transport message for HttpMuleMessageFactory";
    }

    @Override
    public void testValidPayload() throws Exception
    {
        MuleMessageFactory factory = createMuleMessageFactory();
        
        Object payload = getValidTransportMessage();
        MuleMessage message = factory.create(payload, encoding);
        assertNotNull(message);
        assertEquals("/services/Echo", message.getPayload());
        // note that on this level it's only message factory, and it adds messages from http request to the inbound scope
        assertEquals(HttpConstants.METHOD_GET, message.getInboundProperty(HttpConnector.HTTP_METHOD_PROPERTY));
        assertEquals("foo-value", message.getInboundProperty("foo-header"));
    }
    
    public void testInvalidPayloadOnHttpMuleMessageFactory() throws Exception
    {
        HttpMuleMessageFactory factory = new HttpMuleMessageFactory(muleContext);
        try
        {
            factory.extractPayload(getUnsupportedTransportMessage(), encoding);
            fail("HttpMuleMessageFactory should fail when receiving an invalid payload");
        }
        catch (MessageTypeNotSupportedException mtnse)
        {
            // this one was expected
        }
    }
    
    public void testHttpRequestPostPayload() throws Exception
    {
        HttpMuleMessageFactory factory = (HttpMuleMessageFactory) createMuleMessageFactory();
        factory.setExchangePattern(MessageExchangePattern.ONE_WAY);

        HttpRequest request = createPostHttpRequest();
        MuleMessage message = factory.create(request, encoding);
        assertNotNull(message);
        assertEquals(byte[].class, message.getPayload().getClass());
        byte[] payload = (byte[]) message.getPayload();
        assertTrue(Arrays.equals(TEST_MESSAGE.getBytes(), payload));
    }

    private HttpRequest createPostHttpRequest() throws Exception
    {
        String line = REQUEST_LINE.replace(HttpConstants.METHOD_GET, HttpConstants.METHOD_POST);
        RequestLine requestLine = RequestLine.parseLine(line);
        InputStream stream = new ByteArrayInputStream(TEST_MESSAGE.getBytes());
        return new HttpRequest(requestLine, HEADERS, stream, encoding);
    }
    
    public void testHttpMethodGet() throws Exception
    {
        InputStream body = new ByteArrayInputStream("/services/Echo".getBytes());
        HttpMethod method = createMockHttpMethod(HttpConstants.METHOD_GET, body);
        
        MuleMessageFactory factory = createMuleMessageFactory();
        MuleMessage message = factory.create(method, encoding);
        assertNotNull(message);
        assertEquals("/services/Echo", message.getPayloadAsString());
        assertEquals(HttpConstants.METHOD_GET, message.getInboundProperty(HttpConnector.HTTP_METHOD_PROPERTY));
        assertEquals(HttpVersion.HTTP_1_1.toString(), message.getInboundProperty(HttpConnector.HTTP_VERSION_PROPERTY));
        assertEquals("200", message.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));
    }
    
    public void testHttpMethodPost() throws Exception
    {
        InputStream body = new ByteArrayInputStream(TEST_MESSAGE.getBytes());
        HttpMethod method = createMockHttpMethod(HttpConstants.METHOD_POST, body);

        MuleMessageFactory factory = createMuleMessageFactory();
        MuleMessage message = factory.create(method, encoding);
        assertNotNull(message);
        assertEquals(TEST_MESSAGE, message.getPayloadAsString());
        assertEquals(HttpConstants.METHOD_POST, message.getInboundProperty(HttpConnector.HTTP_METHOD_PROPERTY));
        assertEquals(HttpVersion.HTTP_1_1.toString(), message.getInboundProperty(HttpConnector.HTTP_VERSION_PROPERTY));
        assertEquals("200", message.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));
    }
    
    private HttpMethod createMockHttpMethod(String method, InputStream body) throws Exception
    {
        HttpMethod httpMethod = mock(HttpMethod.class);
        when(httpMethod.getName()).thenReturn(method);
        when(httpMethod.getStatusLine()).thenReturn(new StatusLine("HTTP/1.1 200 OK"));
        when(httpMethod.getStatusCode()).thenReturn(HttpConstants.SC_OK);
        when(httpMethod.getURI()).thenReturn(new URI("http://localhost/services/Echo", false));
        when(httpMethod.getResponseHeaders()).thenReturn(HEADERS);
        when(httpMethod.getResponseBodyAsStream()).thenReturn(body);
        
        return httpMethod;
    }
}
