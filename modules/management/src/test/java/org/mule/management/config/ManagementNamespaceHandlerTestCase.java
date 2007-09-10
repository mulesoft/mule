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

import org.mule.impl.internal.admin.EndpointNotificationLoggerAgent;
import org.mule.impl.internal.admin.Log4jNotificationLoggerAgent;
import org.mule.management.agents.JmxAgent;
import org.mule.management.agents.JmxServerNotificationAgent;
import org.mule.management.agents.Log4jAgent;
import org.mule.management.agents.Mx4jAgent;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.manager.UMOAgent;

public class ManagementNamespaceHandlerTestCase extends FunctionalTestCase
{
    private static final int CHAINSAW_PORT = 8080;
    protected String getConfigResources()
    {
        return "management-namespace-config.xml";
    }

    public void testSimpleJmxAgentConfig() throws Exception
    {
        UMOAgent agent = managementContext.getRegistry().lookupAgent("simpleJmxServer");
        assertNotNull(agent);
        assertEquals(JmxAgent.class, agent.getClass());
        JmxAgent jmxAgent = (JmxAgent) agent;
        assertEquals(true, jmxAgent.isCreateServer());
        assertEquals(true, jmxAgent.isLocateServer());
        assertEquals(true, jmxAgent.isEnableStatistics());

        agent = managementContext.getRegistry().lookupAgent("jmx-log4j");
        assertNotNull(agent);
        assertEquals(Log4jAgent.class, agent.getClass());

        agent = managementContext.getRegistry().lookupAgent("jmx-mx4j");
        assertNotNull(agent);
        assertEquals(Mx4jAgent.class, agent.getClass());
        Mx4jAgent mx4jAgent = (Mx4jAgent) agent;
        assertEquals(mx4jAgent.getJmxAdaptorUrl(), "http://127.0.0.1:8000");

        agent = managementContext.getRegistry().lookupAgent("jmxNotificationAgent");
        assertNotNull(agent);
        assertEquals(JmxServerNotificationAgent.class, agent.getClass());

        agent = managementContext.getRegistry().lookupAgent("log4JNotificationAgent");
        assertNotNull(agent);
        assertEquals(Log4jNotificationLoggerAgent.class, agent.getClass());

        agent = managementContext.getRegistry().lookupAgent("chainsawNotificationAgent");
        assertNotNull(agent);
        assertEquals(Log4jNotificationLoggerAgent.class, agent.getClass());
        Log4jNotificationLoggerAgent lnlAgent = (Log4jNotificationLoggerAgent) agent;
        assertEquals(lnlAgent.getChainsawPort(), CHAINSAW_PORT);
        assertEquals(lnlAgent.getChainsawHost(), "127.0.0.1");

        agent = managementContext.getRegistry().lookupAgent("publishNotificationAgent");
        assertNotNull(agent);
        assertEquals(EndpointNotificationLoggerAgent.class, agent.getClass());
        EndpointNotificationLoggerAgent enlAgent = (EndpointNotificationLoggerAgent) agent;
        assertEquals(enlAgent.getEndpointAddress(), "test://test");
    }

    
    

}


