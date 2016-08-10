/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.management.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.compatibility.core.agent.EndpointNotificationLoggerAgent;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.agent.Agent;
import org.mule.runtime.core.api.registry.Registry;
import org.mule.runtime.module.management.agent.JmxApplicationAgent;

import java.util.Collection;
import java.util.Iterator;

import org.junit.Test;

public class ManagementNamespaceHandlerTestCase extends FunctionalTestCase
{
    public ManagementNamespaceHandlerTestCase()
    {
        super();
        // do not start the muleContext, we're only doing registry lookups in this test case
        setStartContext(false);
    }

    @Override
    protected String getConfigFile()
    {
        return "management-namespace-config.xml";
    }

    @Test
    public void testSimpleJmxAgentConfig() throws Exception
    {
        Agent agent = muleContext.getRegistry().lookupObject(JmxApplicationAgent.class);
        agent = muleContext.getRegistry().lookupAgent("publish-notifications");
        assertNotNull(agent);
        assertEquals(EndpointNotificationLoggerAgent.class, agent.getClass());
        EndpointNotificationLoggerAgent enlAgent = (EndpointNotificationLoggerAgent) agent;
        assertEquals(enlAgent.getEndpoint().getEndpointURI().toString(), "test://test");
    }

}
