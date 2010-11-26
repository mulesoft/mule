/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.functional;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HttpEncodingFunctionalTestCase extends HttpFunctionalTestCase
{
    protected static String TEST_MESSAGE = "Test Http Request (R�dgr�d), 57 = \u06f7\u06f5 in Arabic";
    private static String TEST_JAPANESE_MESSAGE = "\u3042";
    
    public HttpEncodingFunctionalTestCase()
    {
        super();
        setDisposeManagerPerSuite(true);
    }
    
    @Override
    protected String getConfigResources()
    {
        return "http-encoding-test.xml";
    }

    // TODO MULE-3690 make me run green
    public void _testPost() throws Exception
    {
        testPost(Locale.US, "US-ASCII", "A");
        testPost(Locale.US, "UTF-8", "A");
        
        testPost(Locale.JAPAN, "UTF-8", TEST_JAPANESE_MESSAGE);
        testPost(Locale.JAPAN, "Shift_JIS", TEST_JAPANESE_MESSAGE);
        testPost(Locale.JAPAN, "Windows-31J", TEST_JAPANESE_MESSAGE);
        testPost(Locale.JAPAN, "EUC-JP", TEST_JAPANESE_MESSAGE);
    }
    
    private void testPost(Locale locale, String encoding, String payload) throws Exception 
    {
        MuleClient client = new MuleClient(muleContext);
        
        Map<String, Object> messageProperties = new HashMap<String, Object>();
        
        String contentType = "text/plain;charset=" + encoding;
        messageProperties.put(HttpConstants.HEADER_CONTENT_TYPE, contentType);
        messageProperties.put(HttpConnector.HTTP_METHOD_PROPERTY, "POST");
     
        String endpointUri = "clientEndpoint." + encoding;
        MuleMessage reply = client.send(endpointUri, payload, messageProperties);
        assertNotNull(reply);
        assertEquals("200", reply.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));
        assertEquals(payload + " Received", reply.getPayloadAsString());
        assertEquals("text/plain;charset=" + encoding, reply.getInboundProperty(HttpConstants.HEADER_CONTENT_TYPE).toString());
        assertEquals(encoding, reply.getEncoding());
    }

    // TODO MULE-3690 make me run green
    public void _testGet() throws Exception 
    {
        testGet(Locale.US, "US-ASCII", "A");
        testGet(Locale.US, "UTF-8", "A");

        testGet(Locale.JAPAN, "UTF-8", TEST_JAPANESE_MESSAGE);
        testGet(Locale.JAPAN, "Shift_JIS", TEST_JAPANESE_MESSAGE);
        testGet(Locale.JAPAN, "Windows-31J", TEST_JAPANESE_MESSAGE);
        testGet(Locale.JAPAN, "EUC-JP", TEST_JAPANESE_MESSAGE);
    }
    
    private void testGet(Locale locale, String encoding, String payload) throws Exception 
    {
        MuleClient client = new MuleClient(muleContext);
        
        Map<String, Object> messageProperties = new HashMap<String, Object>();
        String contentType = "text/plain;charset=" + encoding;
        messageProperties.put(HttpConstants.HEADER_CONTENT_TYPE, contentType);
        messageProperties.put(HttpConnector.HTTP_METHOD_PROPERTY, "GET");
        
        String endpointUri = "clientEndpoint." + encoding;
        MuleMessage reply = client.send(endpointUri, payload, messageProperties);
        String expectedReplyMessage = "/" + encoding + "?body=" + URLEncoder.encode(payload, encoding);

        assertNotNull(reply);
        assertEquals("200", reply.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));
        assertEquals(expectedReplyMessage + " Received", reply.getPayloadAsString());
        assertEquals("text/plain;charset=" + encoding, reply.getInboundProperty(HttpConstants.HEADER_CONTENT_TYPE).toString());
        assertEquals(encoding, reply.getEncoding());
    }

    @Override
    public void testSend() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        
        Map<String, Object> messageProperties = new HashMap<String, Object>();
        messageProperties.put(HttpConstants.HEADER_CONTENT_TYPE, getSendEncoding());
        
        MuleMessage reply = client.send("clientEndpoint", TEST_MESSAGE, messageProperties);
        assertNotNull(reply);
        assertEquals("200", reply.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));
        assertEquals("text/baz;charset=UTF-16BE", reply.<String>getInboundProperty(HttpConstants.HEADER_CONTENT_TYPE));
        assertEquals("UTF-16BE", reply.getEncoding());
        assertEquals(TEST_MESSAGE + " Received", reply.getPayloadAsString());
    }

    protected String getSendEncoding()
    {
        return "text/plain;charset=UTF-8";
    }
}
