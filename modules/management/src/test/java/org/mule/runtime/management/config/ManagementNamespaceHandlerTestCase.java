/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.management.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.agent.Agent;
import org.mule.runtime.core.api.registry.Registry;
import org.mule.runtime.module.management.agent.JmxApplicationAgent;
import org.mule.runtime.module.management.agent.JmxServerNotificationAgent;
import org.mule.runtime.module.management.agent.Log4jAgent;
import org.mule.runtime.module.management.agent.Mx4jAgent;
import org.mule.tck.testmodels.mule.TestAgent;

import java.util.Collection;
import java.util.Iterator;

import org.junit.Test;

public class ManagementNamespaceHandlerTestCase extends FunctionalTestCase
{
    private static final int CHAINSAW_PORT = 8080;

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
        assertNotNull(agent);
        assertEquals(JmxApplicationAgent.class, agent.getClass());
        JmxApplicationAgent jmxAgent = (JmxApplicationAgent) agent;
        assertFalse(jmxAgent.isCreateServer());
        assertTrue(jmxAgent.isLocateServer());
        assertTrue(jmxAgent.isEnableStatistics());
        assertEquals("some://test.url", jmxAgent.getConnectorServerUrl());

        agent = muleContext.getRegistry().lookupAgent("jmx-log4j");
        assertNotNull(agent);
        assertEquals(Log4jAgent.class, agent.getClass());

        agent = muleContext.getRegistry().lookupAgent("jmx-mx4j-adaptor");
        assertNotNull(agent);
        assertEquals(Mx4jAgent.class, agent.getClass());
        Mx4jAgent mx4jAgent = (Mx4jAgent) agent;
        assertEquals(mx4jAgent.getJmxAdaptorUrl(), "http://127.0.0.1:8000");

        agent = muleContext.getRegistry().lookupAgent("jmx-notifications");
        assertNotNull(agent);
        assertEquals(JmxServerNotificationAgent.class, agent.getClass());

        agent = muleContext.getRegistry().lookupAgent("test-custom-agent");
        assertNotNull(agent);
        assertEquals(TestAgent.class, agent.getClass());
        assertEquals("woggle", ((TestAgent) agent).getFrobbit());

        // needs profiler installed
//        agent = muleContext.getRegistry().lookupAgent("yourkit-profiler");
//        assertNotNull(agent);
//        assertEquals(YourKitProfilerAgent.class, agent.getClass());
    }

}
