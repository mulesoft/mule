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

import org.mule.api.agent.Agent;
import org.mule.module.management.agent.JmxAgent;
import org.mule.module.management.agent.JmxServerNotificationAgent;
import org.mule.module.management.agent.Log4jAgent;
import org.mule.tck.FunctionalTestCase;

import java.util.Collection;

import javax.management.MBeanServer;
import javax.management.ObjectName;

public class ManagementDefaultNamespaceHandlerTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "management-default-namespace-config.xml";
    }

    public void testDefaultJmxAgentConfig() throws Exception
    {
        Agent agent = muleContext.getRegistry().lookupAgent("jmx-agent");
        assertNotNull(agent);
        assertEquals(JmxAgent.class, agent.getClass());
        JmxAgent jmxAgent = (JmxAgent) agent;
        
        assertEquals(true, jmxAgent.isCreateServer());
        assertEquals(true, jmxAgent.isLocateServer());
        assertEquals(true, jmxAgent.isEnableStatistics());

        MBeanServer mBeanServer = jmxAgent.getMBeanServer();
        String domainName = jmxAgent.getJmxSupportFactory().getJmxSupport().getDomainName(muleContext);
        Collection beans = mBeanServer.queryMBeans(ObjectName.getInstance(domainName + ":*"), null);
        String message = domainName + ": " + beans.toString();
        assertEquals(message, 6, beans.size());

        agent = muleContext.getRegistry().lookupAgent("jmx-log4j");
        assertNotNull(agent);
        assertEquals(Log4jAgent.class, agent.getClass());

        agent = muleContext.getRegistry().lookupAgent(JmxServerNotificationAgent.DEFAULT_AGENT_NAME);
        assertNotNull(agent);
        assertEquals(JmxServerNotificationAgent.class, agent.getClass());

        agent = muleContext.getRegistry().lookupAgent("jmx-default-config");
        // see TODO in agent
//        assertNull(agent);
        
        //Assertion to check that all Mule MBeans were unregistered during disposal phase.
        muleContext.dispose();
        assertEquals(0, mBeanServer.queryMBeans(ObjectName.getInstance(domainName + ":*"), null).size());
    
    }

}
