/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.module.client.MuleClient;
import org.mule.module.client.RemoteDispatcher;
import org.mule.module.xml.transformer.wire.XStreamWireFormat;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.testmodels.services.Person;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class MuleClientRemotingAxisTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @Rule
    public DynamicPort dynamicPort3 = new DynamicPort("port3");

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/client/axis-client-test-mule-config.xml";
    }
    
    /**
     * Get the Mule address for a Mule client call
     * 
     * @param muleClient The MuleClient instance to use
     * @param inboundEndpointName The inbound endpoint which contains the address
     * @return A String of the 'Mule' address, which in this case should include
     *         'axis" + 'http://<url>'
     */
    private String getMuleAddress(MuleClient muleClient, String inboundEndpointName)
    {
        return ((InboundEndpoint) muleClient.getMuleContext().getRegistry().lookupObject(inboundEndpointName)).getProtocol()
               + ":"
               + ((InboundEndpoint) muleClient.getMuleContext().getRegistry().lookupObject(
                   inboundEndpointName)).getAddress();
    }

    @Test
    public void testRequestResponse() throws Throwable
    {
        MuleClient client = new MuleClient(muleContext);
        RemoteDispatcher dispatcher = client.getRemoteDispatcher("remoteEndpoint");
        try
        {
            MuleMessage result = dispatcher.sendRemote(
                getMuleAddress(client, "inMyComponent2") + "/mycomponent2?method=echo", "test", null);
            assertNotNull(result);
            assertEquals("test", result.getPayloadAsString());
        }
        finally
        {
            client.dispose();
        }
    }

    @Test
    @Ignore("Disabled because of MULE-4844")
    public void testRequestResponseComplex() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        RemoteDispatcher dispatcher = client.getRemoteDispatcher("remoteEndpoint");
        dispatcher.setWireFormat(createObject(XStreamWireFormat.class));

        try
        {
            MuleMessage result = dispatcher.sendRemote(
                getMuleAddress(client, "inMyComponent3") + "/mycomponent3?method=getPerson", "Fred", null);
            assertNotNull(result);
            assertTrue(result.getPayload() instanceof Person);
            assertEquals("Fred", ((Person)result.getPayload()).getFirstName());
            assertEquals("Flintstone", ((Person)result.getPayload()).getLastName());
        }
        finally
        {
            client.dispose();
        }
    }

    @Test
    @Ignore("Disabled because of MULE-4844")
    public void testRequestResponseComplex2() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        RemoteDispatcher dispatcher = client.getRemoteDispatcher("remoteEndpoint");
        dispatcher.setWireFormat(createObject(XStreamWireFormat.class));

        try
        {
            String[] args = new String[]{"Betty", "Rubble"};
            MuleMessage result = dispatcher.sendRemote(
                getMuleAddress(client, "inMyComponent3") + "/mycomponent3?method=addPerson", args, null);
            assertNotNull(result);
            assertTrue(result.getPayload() instanceof Person);
            assertEquals("Betty", ((Person)result.getPayload()).getFirstName());
            assertEquals("Rubble", ((Person)result.getPayload()).getLastName());

            // do a receive
            result = client.send(getMuleAddress(client, "inMyComponent3") + "/mycomponent3?method=getPerson",
                "Betty", null);
            assertNotNull(result);
            assertTrue(result.getPayload() instanceof Person);
            assertEquals("Betty", ((Person)result.getPayload()).getFirstName());
            assertEquals("Rubble", ((Person)result.getPayload()).getLastName());
        }
        finally
        {
            client.dispose();
        }
    }

}
