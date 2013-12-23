/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.client;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class MuleClientTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/client/test-client-mule-config.xml";
    }

    @Test
    public void testClientSendDirect() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        MuleMessage message = client.sendDirect("TestReceiverUMO", null, "Test Client Send message", null);
        assertNotNull(message);
        assertEquals("Test Client Send message Received", message.getPayload());
    }

    @Test
    public void testClientDispatchDirect() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        client.dispatchDirect("TestReceiverUMO", "Test Client dispatch message", null);
    }

    @Test
    public void testClientSendGlobalEndpoint() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        MuleMessage message = client.send("vmEndpoint", "Test Client Send message", null);
        assertNotNull(message);
        assertEquals("Test Client Send message Received", message.getPayload());
    }

    @Test
    public void testClientSend() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        MuleMessage message = client.send(getDispatchUrl(), "Test Client Send message", null);
        assertNotNull(message);
        assertEquals("Test Client Send message Received", message.getPayload());
    }

    @Test
    public void testClientMultiSend() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        for (int i = 0; i < 100; i++)
        {
            MuleMessage message = client.send(getDispatchUrl(), "Test Client Send message " + i, null);
            assertNotNull(message);
            assertEquals("Test Client Send message " + i + " Received", message.getPayload());
        }
    }

    @Test
    public void testClientMultidispatch() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        int i = 0;
        // to init
        client.dispatch(getDispatchUrl(), "Test Client Send message " + i, null);
        long start = System.currentTimeMillis();
        for (i = 0; i < 100; i++)
        {
            client.dispatch(getDispatchUrl(), "Test Client Send message " + i, null);
        }
        long time = System.currentTimeMillis() - start;
        logger.debug(i + " took " + time + "ms to process");
        Thread.sleep(1000);
    }

    public String getDispatchUrl()
    {
        return "vm://test.queue";
    }
}
