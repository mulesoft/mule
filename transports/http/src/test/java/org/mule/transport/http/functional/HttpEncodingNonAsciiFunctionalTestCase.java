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

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.LocaleMessageHandler;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.transformer.AbstractTransformer;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.util.concurrent.Latch;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import junit.framework.Assert;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

public class HttpEncodingNonAsciiFunctionalTestCase extends FunctionalTestCase
{
    private static final String CONTENT_TYPE_HEADER = "text/plain; charset=ISO-2022-JP";

    @Override
    protected String getConfigResources()
    {
        return "http-encoding-non-ascii-test.xml";
    }

    public void testSendViaGET() throws Exception
    {
        Latch latch = new Latch();
        setupAssertIncomingMessage(HttpConstants.METHOD_GET, latch, CONTENT_TYPE_HEADER);
        
        String testMessage = getTestMessage(Locale.JAPAN);
        String encodedPayload = URLEncoder.encode(testMessage, "ISO-2022-JP");
        String url = String.format("http://localhost:60224/get?%1s=%2s",
            HttpConnector.DEFAULT_HTTP_GET_BODY_PARAM_PROPERTY, encodedPayload);
        
        GetMethod method = new GetMethod(url);
        method.addRequestHeader(HttpConstants.HEADER_CONTENT_TYPE, CONTENT_TYPE_HEADER);
        int status = new HttpClient().executeMethod(method);
        assertEquals(HttpConstants.SC_OK, status);
        
        assertTrue(latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        String expected = testMessage + " Received";
        String response = method.getResponseBodyAsString();
        assertEquals(expected, response);
    }
    
    public void testSendByPost() throws Exception
    {
        Object messagePayload = getTestMessage(Locale.JAPAN);
        Map<String, Object> messageProperties = new HashMap<String, Object>();
        messageProperties.put(MuleProperties.MULE_ENCODING_PROPERTY, "ISO-2022-JP");
        doTestSend("POST", messagePayload, messageProperties, CONTENT_TYPE_HEADER);
    }

    private void doTestSend(String method, Object messagePayload, Map<String, Object> messageProperties,
        String expectedContentType) throws Exception
    {
        Latch latch = new Latch();

        setupAssertIncomingMessage(method, latch, expectedContentType);

        MuleClient client = new MuleClient(muleContext);
        MuleMessage reply = client.send("vm://sendBy" + method, messagePayload, messageProperties);

        assertTrue(latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        assertNotNull(reply);
        assertEquals(expectedContentType, reply.getInvocationProperty(HttpConstants.HEADER_CONTENT_TYPE));
        assertEquals("EUC-JP", reply.getEncoding());
        assertEquals(getTestMessage(Locale.JAPAN) + " Received", reply.getPayloadAsString());
    }

    private void setupAssertIncomingMessage(String method, final Latch latch,
        final String expectedContentType) throws Exception
    {
        FunctionalTestComponent ftc = getFunctionalTestComponent("testReceive" + method);
        ftc.setEventCallback(new EventCallback()
        {
            public void eventReceived(MuleEventContext context, Object serviceComponent) throws Exception
            {
                MuleMessage message = context.getMessage();

                Assert.assertEquals(expectedContentType,
                    message.getInboundProperty(HttpConstants.HEADER_CONTENT_TYPE, null));
                Assert.assertEquals("ISO-2022-JP", message.getEncoding());

                Object payload = message.getPayload();
                if (payload instanceof String)
                {
                    assertEquals(getTestMessage(Locale.JAPAN), payload);
                }
                else if (payload instanceof byte[])
                {
                    String payloadString = new String((byte[])payload, message.getEncoding());
                    assertEquals(getTestMessage(Locale.JAPAN), payloadString);
                }
                else
                {
                    fail();
                }

                latch.countDown();
            }
        });
    }

    public static class ParamMapToString extends AbstractTransformer
    {
        @Override
        @SuppressWarnings("unchecked")
        protected Object doTransform(Object src, String outputEncoding) throws TransformerException
        {
            Map<String, Object> map = (Map<String, Object>)src;
            return map.get(HttpConnector.DEFAULT_HTTP_GET_BODY_PARAM_PROPERTY);
        }
    }

    String getTestMessage(Locale locale)
    {
        return LocaleMessageHandler.getString("test-data", locale,
            "HttpEncodingNonAsciiFunctionalTestCase.getMessage", new Object[]{});
    }
}
