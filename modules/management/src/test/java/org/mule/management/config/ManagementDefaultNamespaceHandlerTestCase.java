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

import org.mule.management.agents.JmxAgent;
import org.mule.management.agents.JmxServerNotificationAgent;
import org.mule.management.agents.Log4jAgent;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.manager.UMOAgent;

public class ManagementDefaultNamespaceHandlerTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "management-default-namespace-config.xml";
    }

    public void testDefaultJmxAgentConfig() throws Exception
    {
        UMOAgent agent = managementContext.getRegistry().lookupAgent("JMX Agent");
        assertNotNull(agent);
        assertEquals(JmxAgent.class, agent.getClass());
        JmxAgent jmxAgent = (JmxAgent) agent;
        assertEquals(true, jmxAgent.isCreateServer());
        assertEquals(true, jmxAgent.isLocateServer());
        assertEquals(true, jmxAgent.isEnableStatistics());

        agent = managementContext.getRegistry().lookupAgent("Log4j JMX Agent");
        assertNotNull(agent);
        assertEquals(Log4jAgent.class, agent.getClass());

        agent = managementContext.getRegistry().lookupAgent(JmxServerNotificationAgent.DEFAULT_AGENT_NAME);
        assertNotNull(agent);
        assertEquals(JmxServerNotificationAgent.class, agent.getClass());

        agent = managementContext.getRegistry().lookupAgent("Default Jmx Agent Support");
        assertNull(agent);
    }

}
