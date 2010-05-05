/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http;

import org.mule.api.MuleMessage;
import org.mule.api.transport.MessageTypeNotSupportedException;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.api.transport.PropertyScope;
import org.mule.transport.AbstractMuleMessageFactoryTestCase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import edu.emory.mathcs.backport.java.util.Arrays;

import org.apache.commons.httpclient.Header;

public class HttpMuleMessageFactoryTestCase extends AbstractMuleMessageFactoryTestCase
{
    private static final String REQUEST_LINE = "GET /services/Echo HTTP/1.1";
    private static final Header[] HEADERS = new Header[] { new Header("foo-header", "foo-value") };

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
        assertEquals("foo-value", message.getProperty("foo-header", PropertyScope.INBOUND));
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
        factory.setSynchronous(false);

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
    
    public void _testHttpMethodGet() throws Exception
    {
        // TODO MessageAdapterRemoval: implement me
    }
    
    public void _testHttpMethodPost() throws Exception
    {
        // TODO MessageAdapterRemoval: implement me
    }
}
