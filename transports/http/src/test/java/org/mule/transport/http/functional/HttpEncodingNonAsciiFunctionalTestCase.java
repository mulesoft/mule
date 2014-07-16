/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.config.MuleProperties;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.LocaleMessageHandler;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transformer.AbstractTransformer;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.util.SystemUtils;
import org.mule.util.concurrent.Latch;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Rule;
import org.junit.Test;

public class HttpEncodingNonAsciiFunctionalTestCase extends FunctionalTestCase
{
    private static final String ENCODING_JP = "ISO-2022-JP";
    private static final String FORM_ENCODED_CONTENT_TYPE_HEADER = "application/x-www-form-urlencoded; charset=" + ENCODING_JP;
    private static final String PLAIN_CONTENT_TYPE_HEADER = "text/plain; charset=" + ENCODING_JP;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "http-encoding-non-ascii-test.xml";
    }

    @Override
    protected boolean isDisabledInThisEnvironment()
    {
        // MULE-5268
        return SystemUtils.isIbmJDK();
    }

    @Test
    public void testSendViaGET() throws Exception
    {
        Latch latch = new Latch();
        setupAssertIncomingMessage(HttpConstants.METHOD_GET, latch, PLAIN_CONTENT_TYPE_HEADER);

        String testMessage = getTestMessage(Locale.JAPAN);
        String encodedPayload = URLEncoder.encode(testMessage, "ISO-2022-JP");
        String url = String.format("http://localhost:%1d/get?%2s=%3s",
            dynamicPort.getNumber(), HttpConnector.DEFAULT_HTTP_GET_BODY_PARAM_PROPERTY, encodedPayload);

        GetMethod method = new GetMethod(url);
        method.addRequestHeader(HttpConstants.HEADER_CONTENT_TYPE, PLAIN_CONTENT_TYPE_HEADER);
        int status = new HttpClient().executeMethod(method);
        assertEquals(HttpConstants.SC_OK, status);

        assertTrue(latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        String expected = testMessage + " Received";
        String response = method.getResponseBodyAsString();
        assertEquals(expected, response);

        Header responseContentType = method.getResponseHeader(HttpConstants.HEADER_CONTENT_TYPE);
        assertEquals("text/plain;charset=EUC-JP", responseContentType.getValue());
    }

    @Test
    public void testSendViaPOST() throws Exception
    {
        Object payload = getTestMessage(Locale.JAPAN);

        Map<String, Object> messageProperties = new HashMap<String, Object>();
        messageProperties.put(MuleProperties.MULE_ENCODING_PROPERTY, "ISO-2022-JP");

        doTestSend(HttpConstants.METHOD_POST, payload, messageProperties, PLAIN_CONTENT_TYPE_HEADER);
    }

    @Test
    public void testSendViaPostMap() throws Exception
    {
        Map<String, Object> messagePayload = new HashMap<String, Object>();
        messagePayload.put("body", getTestMessage(Locale.JAPAN));

        Map<String, Object> messageProperties = new HashMap<String, Object>();
        messageProperties.put(MuleProperties.MULE_ENCODING_PROPERTY, "ISO-2022-JP");
        doTestSend("POSTMap", messagePayload, messageProperties, FORM_ENCODED_CONTENT_TYPE_HEADER);
    }

    private void doTestSend(String method, Object messagePayload, Map<String, Object> messageProperties,
        String expectedContentTypeHeader) throws Exception
    {
        Latch latch = new Latch();

        setupAssertIncomingMessage(method, latch, expectedContentTypeHeader);

        MuleClient client = muleContext.getClient();
        MuleMessage reply = client.send("vm://sendBy" + method, messagePayload, messageProperties);

        assertTrue(latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        assertNotNull(reply);
        assertEquals("EUC-JP", reply.getEncoding());
        assertEquals(getTestMessage(Locale.JAPAN) + " Received", reply.getPayloadAsString());
    }

    private void setupAssertIncomingMessage(String method, final Latch latch,
        final String expectedContentTypeHeader) throws Exception
    {
        FunctionalTestComponent ftc = getFunctionalTestComponent("testReceive" + method);
        ftc.setEventCallback(new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object serviceComponent) throws Exception
            {
                MuleMessage message = context.getMessage();

                Assert.assertEquals(expectedContentTypeHeader,
                    message.getInboundProperty(HttpConstants.HEADER_CONTENT_TYPE, null));
                Assert.assertEquals("ISO-2022-JP", message.getEncoding());

                Object payload = message.getPayload();
                if (payload instanceof String)
                {
                    assertEquals(getTestMessage(Locale.JAPAN), payload);
                }
                else
                {
                    fail();
                }

                latch.countDown();
            }
        });
    }

    private String getTestMessage(Locale locale)
    {
        return LocaleMessageHandler.getString("test-data", locale,
            "HttpEncodingNonAsciiFunctionalTestCase.getMessage", new Object[]{});
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
}
