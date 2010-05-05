/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.tcp;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class TcpFunctionalTestCase extends FunctionalTestCase 
{

    protected static String TEST_MESSAGE = "Test TCP Request";

    protected String getConfigResources()
    {
        return "tcp-functional-test.xml";
    }

    public void testSend() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage result = client.send("clientEndpoint", TEST_MESSAGE, null);
        assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());
    }

    public void testDispatchAndReply() throws Exception
    {
        MuleClient client = new MuleClient();
        client.dispatch("asyncClientEndpoint", TEST_MESSAGE, null);
        // MULE-2754
        Thread.sleep(100);
        MuleMessage result =  client.request("asyncClientEndpoint", 10000);
        assertNotNull(result);
        assertEquals(TEST_MESSAGE + " Received Async", result.getPayloadAsString());
    }

    public void timeMultipleSend() throws Exception
    {
        MuleClient client = new MuleClient();
        long now = System.currentTimeMillis();
        int count = 1000;
        for (int i = 0; i < count; i++)
        {
            MuleMessage result = client.send("clientEndpoint", TEST_MESSAGE, null);
            assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());
        }
        long later = System.currentTimeMillis();
        double speed = count * 1000.0 / (later - now);
        logger.error(speed + " messages per second");
    }

}
