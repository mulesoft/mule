/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp.integration;

import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

/**
 * This test was set for the new changes due to Mule1199
 */
public class MuleMessageProtocolChunkingTestCase extends FunctionalTestCase
{
    private static int messages = 2;
    private static int messagelength = 10;

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

    public void testHugeChunk() throws Exception
    {
        StringBuffer message = new StringBuffer();
        // send 50K of stuff;
        for (int i = 1000; i < 2000; i++)
        {
            message.append(i);
        }
        sendString(message.toString());
    }

    public void testCustomObject() throws Exception
    {
        MuleClient client = new MuleClient();
        StringBuffer sBuffer = new StringBuffer();
        // send 50K of stuff;
        for (int i = 10000; i < 20000; i++)
        {
            sBuffer.append(i);
        }
        MessageObject message = new MessageObject(1, sBuffer.toString(), true);

        for (int i = 0; i < messages; i++)
        {
            client.dispatch("vm://in", new MuleMessage(message));
        }

        for (int i = 0; i < messages; i++)
        {
            UMOMessage msg = client.receive("vm://out", 30000);
            assertTrue(msg.getPayload() instanceof MessageObject);
            MessageObject received = (MessageObject)msg.getPayload();
            assertEquals(message.s, received.s);
            assertEquals(1, received.i);
            assertEquals(true, received.b);
        }
    }

    private void sendString(String message) throws Exception
    {
        MuleClient client = new MuleClient();

        for (int i = 0; i < messages; i++)
        {
            client.dispatch("vm://in", new MuleMessage(message));
        }
        for (int i = 0; i < messages; i++)
        {
            UMOMessage msg = client.receive("vm://out", 30000);
            assertEquals(message, new String((byte[])msg.getPayload()));
        }
    }

    protected String getConfigResources()
    {
        return "MuleMessageProtocol-mule-config.xml";
    }

}
