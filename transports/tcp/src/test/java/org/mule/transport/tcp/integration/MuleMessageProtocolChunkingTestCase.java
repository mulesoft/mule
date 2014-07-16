/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

/**
 * This test was set for the new changes due to Mule1199
 */
public class MuleMessageProtocolChunkingTestCase extends FunctionalTestCase
{
    public static final long WAIT_MS = 3000L;
    private static int messages = 2;
    private static int messagelength = 10;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "mule-message-protocol-mule-config.xml";
    }

    @Test
    public void testChunking() throws Exception
    {
        String message = "";
        for (int i = 0; i < messagelength; i++)
        {
            for (int j = 0; j < 10; j++)
                message += i;
        }
        sendString(message);
    }

    @Test
    public void testHugeChunk() throws Exception
    {
        StringBuilder message = new StringBuilder();
        // send 50K of stuff;
        for (int i = 1000; i < 2000; i++)
        {
            message.append(i);
        }
        sendString(message.toString());
    }

    @Test
    public void testCustomObject() throws Exception
    {
        MuleClient client = muleContext.getClient();
        StringBuilder sBuffer = new StringBuilder();
        // send 50K of stuff;
        for (int i = 10000; i < 20000; i++)
        {
            sBuffer.append(i);
        }
        MessageObject message = new MessageObject(1, sBuffer.toString(), true);

        for (int i = 0; i < messages; i++)
        {
            client.dispatch("vm://in", new DefaultMuleMessage(message, muleContext));
        }

        for (int i = 0; i < messages; i++)
        {
            MuleMessage msg = client.request("vm://out", WAIT_MS);
            assertNotNull(msg);
            assertTrue(msg.getPayload() instanceof MessageObject);
            MessageObject received = (MessageObject)msg.getPayload();
            assertEquals(message.s, received.s);
            assertEquals(1, received.i);
            assertEquals(true, received.b);
        }
    }

    private void sendString(String message) throws Exception
    {
        MuleClient client = muleContext.getClient();

        for (int i = 0; i < messages; i++)
        {
            client.dispatch("vm://in", new DefaultMuleMessage(message, muleContext));
        }
        for (int i = 0; i < messages; i++)
        {
            MuleMessage msg = client.request("vm://out", WAIT_MS);
            assertEquals(message, new String((byte[])msg.getPayload()));
        }
    }
}
