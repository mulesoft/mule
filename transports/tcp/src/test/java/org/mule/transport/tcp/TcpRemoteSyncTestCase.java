/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TcpRemoteSyncTestCase extends AbstractServiceAndFlowTestCase
{
    public static final String message = "mule";

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @Rule
    public DynamicPort dynamicPort3 = new DynamicPort("port3");


    public TcpRemoteSyncTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE, "tcp-remotesync-service.xml"},
            {ConfigVariant.FLOW, "tcp-remotesync-flow.xml"}});
    }

    @Test
    public void testTcpTcpRemoteSync() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map<String, Object> props = new HashMap<String, Object>();

        // must notify the client to wait for a response from the server
        props.put(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, Boolean.TRUE);
        MuleMessage reply = client.send(((InboundEndpoint) client.getMuleContext().getRegistry().lookupObject("echoInTcp")).getAddress(), 
            new DefaultMuleMessage(message, muleContext), props);

        assertNotNull(reply);
        assertNotNull(reply.getPayload());
        assertEquals("Received: " + message, reply.getPayloadAsString());
    }

    @Test
    public void testTcpVmRemoteSync() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map<String, Object> props = new HashMap<String, Object>();

        //must notify the client to wait for a response from the server
        props.put(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, Boolean.TRUE);

        MuleMessage reply = client.send(((InboundEndpoint) client.getMuleContext().getRegistry().lookupObject("echo2InTcp")).getAddress(),
            new DefaultMuleMessage(message, muleContext), props);

        assertNotNull(reply);
        assertNotNull(reply.getPayload());
        assertEquals("Received: " + message, reply.getPayloadAsString());
    }

}
