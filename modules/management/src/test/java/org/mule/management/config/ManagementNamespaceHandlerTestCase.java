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
import org.mule.management.support.JmxSupportFactory;
import org.mule.management.support.AutoDiscoveryJmxSupportFactory;
import org.mule.management.support.JmxSupport;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.mule.TestAgent;
import org.mule.umo.manager.UMOAgent;
import org.mule.RegistryContext;
import org.mule.config.spring.SpringRegistry;
import org.mule.registry.Registry;

import javax.management.ObjectInstance;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class ManagementNamespaceHandlerTestCase extends FunctionalTestCase
{
    private static final int CHAINSAW_PORT = 8080;
    private MBeanServer mBeanServer;
    protected JmxSupportFactory jmxSupportFactory = AutoDiscoveryJmxSupportFactory.getInstance();
    protected JmxSupport jmxSupport = jmxSupportFactory.getJmxSupport();

    protected String getConfigResources()
    {
        return "management-namespace-config.xml";
    }

    protected void doSetUp()
    {
        mBeanServer = (MBeanServer) MBeanServerFactory.findMBeanServer(null).get(0);
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

    public void testAgentsOrder() throws Exception
    {
        Registry registry = RegistryContext.getRegistry();
        SpringRegistry springRegistry = (SpringRegistry) registry.getParent();
        assertNotNull(springRegistry);
        Collection agents = springRegistry.lookupObjects(UMOAgent.class);
        assertEquals(agents.size(), 8);
        Iterator iter = agents.iterator();
        assertTrue(iter.next() instanceof JmxAgent);
        assertTrue(iter.next() instanceof Log4jAgent);
        assertTrue(iter.next() instanceof Mx4jAgent);
        assertTrue(iter.next() instanceof TestAgent);
        assertTrue(iter.next() instanceof JmxServerNotificationAgent);
        Log4jNotificationLoggerAgent log4jAgent = (Log4jNotificationLoggerAgent) iter.next();
        assertEquals(log4jAgent.getName(), "log4JNotificationAgent");
        log4jAgent = (Log4jNotificationLoggerAgent) iter.next();
        assertEquals(log4jAgent.getName(), "chainsawNotificationAgent");
        assertTrue(iter.next() instanceof EndpointNotificationLoggerAgent);
    }

    private void unregisterMBeansByMask(final String mask) throws Exception
    {
        Set objectInstances = mBeanServer.queryMBeans(jmxSupport.getObjectName(mask), null);
        for (Iterator it = objectInstances.iterator(); it.hasNext();)
        {
            ObjectInstance instance = (ObjectInstance) it.next();
            mBeanServer.unregisterMBean(instance.getObjectName());
        }
    }

    protected void doTearDown() throws Exception
    {
        unregisterMBeansByMask("*.*:*");
        unregisterMBeansByMask("log4j:*");
        mBeanServer = null;
    }

}


