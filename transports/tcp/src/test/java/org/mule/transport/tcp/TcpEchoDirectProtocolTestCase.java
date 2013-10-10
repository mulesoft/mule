/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

public class TcpEchoDirectProtocolTestCase extends AbstractServiceAndFlowTestCase
{
    protected static String TEST_MESSAGE = "Test TCP Request";

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public TcpEchoDirectProtocolTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);        
    }
    
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "tcp-echo-test-service.xml"},
            {ConfigVariant.FLOW, "tcp-echo-test-flow.xml"}
        });
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
