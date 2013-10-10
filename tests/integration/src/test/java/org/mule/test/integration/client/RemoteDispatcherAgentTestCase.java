/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.client;

import org.mule.module.client.remoting.RemoteDispatcherAgent;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transformer.wire.SerializedMuleMessageWireFormat;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RemoteDispatcherAgentTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/client/mule-remote-dispatcher-agent.xml";
    }

    @Test
    public void testNonEmptyProperties() throws Exception
    {
        RemoteDispatcherAgent agent = (RemoteDispatcherAgent) muleContext.getRegistry().lookupAgent("remote-dispatcher-agent");
        assertNotNull(agent.getEndpoint());
        assertEquals("test://localhost:50608",agent.getEndpoint().getEndpointURI().getUri().toString());
        assertNotNull(agent.getWireFormat());
        assertTrue(agent.getWireFormat() instanceof SerializedMuleMessageWireFormat);
    }
}

