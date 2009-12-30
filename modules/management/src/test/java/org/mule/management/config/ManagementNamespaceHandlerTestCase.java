/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.config;

import org.mule.agent.EndpointNotificationLoggerAgent;
import org.mule.agent.Log4jNotificationLoggerAgent;
import org.mule.api.agent.Agent;
import org.mule.api.registry.Registry;
import org.mule.module.management.agent.JmxAgent;
import org.mule.module.management.agent.JmxServerNotificationAgent;
import org.mule.module.management.agent.Log4jAgent;
import org.mule.module.management.agent.Mx4jAgent;
import org.mule.module.management.support.AutoDiscoveryJmxSupportFactory;
import org.mule.module.management.support.JmxSupport;
import org.mule.module.management.support.JmxSupportFactory;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.mule.TestAgent;

import java.util.Collection;
import java.util.Iterator;

public class ManagementNamespaceHandlerTestCase extends FunctionalTestCase
{
    private static final int CHAINSAW_PORT = 8080;
    protected JmxSupportFactory jmxSupportFactory = AutoDiscoveryJmxSupportFactory.getInstance();
    protected JmxSupport jmxSupport = jmxSupportFactory.getJmxSupport();

    
    public ManagementNamespaceHandlerTestCase()
    {
        super();
        // do not start the muleContext, we're only doing registry lookups in this test case
        setStartContext(false);
    }

    protected String getConfigResources()
    {
        return "management-namespace-config.xml";
    }

    public void testSimpleJmxAgentConfig() throws Exception
    {
        Agent agent = (Agent) muleContext.getRegistry().lookupObject(JmxAgent.class);
        assertNotNull(agent);
        assertEquals(JmxAgent.class, agent.getClass());
        JmxAgent jmxAgent = (JmxAgent) agent;
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

    public void testAgentsOrder() throws Exception
    {
        Registry registry = muleContext.getRegistry();
        assertNotNull(registry);
        Collection agents = registry.lookupObjects(Agent.class);
        assertEquals(agents.size(), 8);
        
        Iterator iter = agents.iterator();
        assertTrue(iter.next() instanceof JmxAgent);
        assertTrue(iter.next() instanceof Log4jAgent);
        assertTrue(iter.next() instanceof Mx4jAgent);
        assertTrue(iter.next() instanceof TestAgent);
        assertTrue(iter.next() instanceof JmxServerNotificationAgent);
        
        Log4jNotificationLoggerAgent log4jAgent = (Log4jNotificationLoggerAgent) iter.next();
        assertEquals(log4jAgent.getName(), "log4j-notifications");
        
        log4jAgent = (Log4jNotificationLoggerAgent) iter.next();
        assertEquals(log4jAgent.getName(), "chainsaw-notifications");
        assertTrue(iter.next() instanceof EndpointNotificationLoggerAgent);
    }

}
