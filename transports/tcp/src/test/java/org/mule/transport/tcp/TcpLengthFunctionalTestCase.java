/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TcpLengthFunctionalTestCase extends FunctionalTestCase
{
    protected static String TEST_MESSAGE = "Test TCP Request";
    private int timeout = 60 * 1000 / 20;

    @ClassRule
    public static DynamicPort dynamicPort1 = new DynamicPort("port1");

    @ClassRule
    public static DynamicPort dynamicPort2 = new DynamicPort("port2");

    @ClassRule
    public static DynamicPort dynamicPort3 = new DynamicPort("port3");
    
    public TcpLengthFunctionalTestCase()
    {
        setDisposeContextPerClass(true);
    }

    @Override
    protected String getConfigResources()
    {
        return "tcp-length-functional-test.xml";
    }

    @Test
    public void testSend() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("clientEndpoint", TEST_MESSAGE, null);
        assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());
    }

    @Test
    public void testDispatchAndReplyViaStream() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("asyncClientEndpoint1", TEST_MESSAGE, null);
        // MULE-2754
        Thread.sleep(200);
        MuleMessage result =  client.request("asyncClientEndpoint1", timeout);
        // expect failure - streaming not supported
        assertNull(result);
    }

    @Test
    public void testDispatchAndReply() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("asyncClientEndpoint2", TEST_MESSAGE, null);
        // MULE-2754
        Thread.sleep(200);
        MuleMessage result =  client.request("asyncClientEndpoint2", timeout);
        // expect failure - TCP simply can't work like this
        assertNull(result);
    }

}
