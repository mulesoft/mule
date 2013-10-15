/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp;

import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TcpEchoDirectProtocolTestCase extends FunctionalTestCase
{

    protected static String TEST_MESSAGE = "Test TCP Request";

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "tcp-echo-test.xml";
    }

    @Test
    public void testSend() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        
        MuleMessage response = client.send(((InboundEndpoint) client.getMuleContext().getRegistry().lookupObject("inBounceTcpMMP")).getAddress(), 
            TEST_MESSAGE, null);
        
        assertNotNull(response);
        assertEquals(TEST_MESSAGE, response.getPayload());
    }

}
