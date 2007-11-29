/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

import java.util.HashMap;
import java.util.Map;

public class TcpFunctionalTestCase extends FunctionalTestCase 
{

    protected static String TEST_MESSAGE = "Test TCP Request";

    public TcpFunctionalTestCase()
    {
        setDisposeManagerPerSuite(true);
    }

    protected String getConfigResources()
    {
        return "tcp-functional-test.xml";
    }

    public void testSend() throws Exception
    {
        MuleClient client = new MuleClient();
        Map props = new HashMap();
        UMOMessage result = client.send("clientEndpoint", TEST_MESSAGE, props);
        assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());
    }

    public void testDispatchAndReply() throws Exception
    {
        MuleClient client = new MuleClient();
        Map props = new HashMap();
        client.dispatch("asyncClientEndpoint", TEST_MESSAGE, props);
        // MULE-2754
        Thread.sleep(2000);
        UMOMessage result =  client.request("asyncClientEndpoint", 10000);
        assertNotNull(result);
        assertEquals(TEST_MESSAGE + " Received Async", result.getPayloadAsString());
    }

    public void timeMultipleSend() throws Exception
    {
        MuleClient client = new MuleClient();
        Map props = new HashMap();
        long now = System.currentTimeMillis();
        int count = 1000;
        for (int i = 0; i < count; i++)
        {
            UMOMessage result = client.send("clientEndpoint", TEST_MESSAGE, props);
            assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());
        }
        long later = System.currentTimeMillis();
        double speed = count * 1000.0 / (later - now);
        logger.error(speed + " messages per second");
    }

}
