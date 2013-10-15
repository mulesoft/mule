/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.transport.DispatchException;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.net.SocketTimeoutException;
import java.util.Date;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * See MULE-4491 "Http outbound endpoint does not use responseTimeout attribute"
 * See MULE-4743 "MuleClient.send() timeout is not respected with http transport"
 */
public class HttpResponseTimeoutTestCase extends FunctionalTestCase
{

    protected static String PAYLOAD = "Eugene";
    protected static int DEFAULT_RESPONSE_TIMEOUT = 2000;
    protected MuleClient muleClient;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "http-response-timeout-config.xml";
    }
    
    protected String getPayload()
    {
        return PAYLOAD;
    }

    protected String getProcessedPayload()
    {
        return getPayload() + " processed";
    }
    
    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        muleClient = new MuleClient(muleContext);
    }

    @Test
    public void testDecreaseOutboundEndpointResponseTimeout() throws Exception
    {
        Date beforeCall = new Date();
        MuleMessage message = muleClient.send("vm://decreaseTimeoutRequest", getPayload(), null);
        Date afterCall = new Date();

        // If everything is good the connection will timeout after 5s and throw an
        // exception. The original unprocessed message is returned in the response
        // message.
        assertNotNull(message);
        assertNotNull(getPayload(), message.getPayloadAsString());
        assertTrue(message.getExceptionPayload().getRootException() instanceof SocketTimeoutException);
        assertTrue((afterCall.getTime() - beforeCall.getTime()) < DEFAULT_RESPONSE_TIMEOUT);
    }

    @Test
    public void testIncreaseOutboundEndpointResponseTimeout() throws Exception
    {
        Date beforeCall = new Date();
        MuleMessage message = muleClient.send("vm://increaseTimeoutRequest", getPayload(), null);
        Date afterCall = new Date();

        // If everything is good the connection will not timeout and the processed
        // message will be returned as the response. There is no exception payload.
        assertNotNull(message);
        assertNull(message.getExceptionPayload());
        assertNotNull(getPayload(), message.getPayloadAsString());
        assertTrue((afterCall.getTime() - beforeCall.getTime()) > DEFAULT_RESPONSE_TIMEOUT);
    }

    @Test
    public void testDecreaseMuleClientSendResponseTimeout() throws Exception
    {
        Date beforeCall = new Date();
        Date afterCall;

        try
        {
            muleClient.send(((InboundEndpoint) muleClient.getMuleContext().getRegistry().lookupObject("inDelayService")).getAddress(), getPayload(), null, 1000);
            fail("SocketTimeoutException expected");
        }
        catch (DispatchException e)
        {
            assertTrue(e.getCause() instanceof SocketTimeoutException);
        }
        // Exception should have been thrown after timeout specified which is
        // less than default.
        afterCall = new Date();
        assertTrue((afterCall.getTime() - beforeCall.getTime()) < DEFAULT_RESPONSE_TIMEOUT);
    }

    @Test
    public void testIncreaseMuleClientSendResponseTimeout() throws Exception
    {
        Date beforeCall = new Date();
        MuleMessage message = muleClient.send(((InboundEndpoint) muleClient.getMuleContext().getRegistry().lookupObject("inDelayService")).getAddress(), getPayload(), null, 3000);
        Date afterCall = new Date();

        // If everything is good the we'll have received a result after more than 10s
        assertNotNull(message);
        assertNull(message.getExceptionPayload());
        assertNotNull(getProcessedPayload(), message.getPayloadAsString());
        assertTrue((afterCall.getTime() - beforeCall.getTime()) > DEFAULT_RESPONSE_TIMEOUT);
    }

}
