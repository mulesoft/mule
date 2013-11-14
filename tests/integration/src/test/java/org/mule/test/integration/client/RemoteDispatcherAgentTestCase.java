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

import org.mule.module.client.remoting.RemoteDispatcherAgent;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transformer.wire.SerializedMuleMessageWireFormat;

import org.junit.Test;

public class RemoteDispatcherAgentTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
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

