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
import org.mule.runtime.core.agent.EndpointNotificationLoggerAgent;
import org.mule.runtime.core.agent.Log4jNotificationLoggerAgent;
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

        agent = muleContext.getRegistry().lookupAgent("log4j-notifications");
        assertNotNull(agent);
        assertEquals(Log4jNotificationLoggerAgent.class, agent.getClass());

        agent = muleContext.getRegistry().lookupAgent("chainsaw-notifications");
        assertNotNull(agent);
        assertEquals(Log4jNotificationLoggerAgent.class, agent.getClass());
        Log4jNotificationLoggerAgent lnlAgent = (Log4jNotificationLoggerAgent) agent;
        assertEquals(lnlAgent.getChainsawPort(), CHAINSAW_PORT);
        assertEquals(lnlAgent.getChainsawHost(), "127.0.0.1");

        agent = muleContext.getRegistry().lookupAgent("publish-notifications");
        assertNotNull(agent);
        assertEquals(EndpointNotificationLoggerAgent.class, agent.getClass());
        EndpointNotificationLoggerAgent enlAgent = (EndpointNotificationLoggerAgent) agent;
        assertEquals(enlAgent.getEndpoint().getEndpointURI().toString(), "test://test");

        agent = muleContext.getRegistry().lookupAgent("test-custom-agent");
        assertNotNull(agent);
        assertEquals(TestAgent.class, agent.getClass());
        assertEquals("woggle", ((TestAgent) agent).getFrobbit());

        // needs profiler installed
//        agent = muleContext.getRegistry().lookupAgent("yourkit-profiler");
//        assertNotNull(agent);
//        assertEquals(YourKitProfilerAgent.class, agent.getClass());
    }

    @Test
    public void testAgentsOrder() throws Exception
    {
        Registry registry = muleContext.getRegistry();
        assertNotNull(registry);
        Collection<Agent> agents = registry.lookupObjects(Agent.class);
        assertEquals(agents.size(), 8);
        
        Iterator<Agent> iter = agents.iterator();
        assertTrue(iter.next() instanceof Log4jAgent);
        assertTrue(iter.next() instanceof Mx4jAgent);
        assertTrue(iter.next() instanceof TestAgent);
        assertTrue(iter.next() instanceof JmxServerNotificationAgent);

        Log4jNotificationLoggerAgent log4jAgent = (Log4jNotificationLoggerAgent) iter.next();
        assertEquals(log4jAgent.getName(), "log4j-notifications");

        log4jAgent = (Log4jNotificationLoggerAgent) iter.next();
        assertEquals(log4jAgent.getName(), "chainsaw-notifications");
        assertTrue(iter.next() instanceof EndpointNotificationLoggerAgent);
        assertTrue(iter.next() instanceof JmxApplicationAgent);
    }

}
