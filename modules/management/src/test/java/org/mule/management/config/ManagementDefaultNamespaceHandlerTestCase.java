/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.api.agent.Agent;
import org.mule.module.management.agent.JmxApplicationAgent;
import org.mule.module.management.agent.JmxServerNotificationAgent;
import org.mule.module.management.agent.Mx4jAgent;
import org.mule.module.management.support.SimplePasswordJmxAuthenticator;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class ManagementDefaultNamespaceHandlerTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "management-default-namespace-config.xml";
    }

    @Test
    public void testDefaultJmxAgentConfig() throws Exception
    {
        Agent agent = muleContext.getRegistry().lookupAgent("jmx-agent");
        assertNotNull(agent);
        assertEquals(JmxApplicationAgent.class, agent.getClass());
        JmxApplicationAgent jmxAgent = (JmxApplicationAgent) agent;
        
        assertEquals(false, jmxAgent.isCreateServer());
        assertEquals(true, jmxAgent.isLocateServer());
        assertEquals(true, jmxAgent.isEnableStatistics());
        assertEquals(SimplePasswordJmxAuthenticator.class, jmxAgent.getJmxAuthenticator().getClass());

        agent = muleContext.getRegistry().lookupAgent(JmxServerNotificationAgent.DEFAULT_AGENT_NAME);
        assertNotNull(agent);
        assertEquals(JmxServerNotificationAgent.class, agent.getClass());


        agent = muleContext.getRegistry().lookupAgent("jmx-mx4j-adaptor");
        assertNotNull(agent);
        assertEquals(Mx4jAgent.class, agent.getClass());

        
        agent = muleContext.getRegistry().lookupAgent("jmx-default-config");
        // see TODO in agent
//        assertNull(agent);
    }

}
