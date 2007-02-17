/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.mule;

import org.mule.MuleManager;
import org.mule.management.agents.JmxAgent;
import org.mule.management.agents.Mx4jAgent;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.manager.UMOManager;

import java.util.List;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

public class MuleAgentsTestCase extends AbstractMuleTestCase
{
    private UMOManager manager;

    /**
     * Print the name of this test to standard output
     */
    protected void doSetUp() throws Exception
    {
        manager = getManager(true);
    }


    protected void doTearDown () throws Exception {
        if (manager != null)
        {
            manager.dispose();
        }
    }

    public void testRemoveNonExistentAgent() throws Exception
    {
        MuleManager.getRegistry().unregisterAgent("DOES_NOT_EXIST");
        // should not throw NPE
    }

    public void testAgentsRegistrationOrder() throws Exception
    {
        manager.setId("MuleAgentsTestCase.agentsRegistrationOrder");
        JmxAgent agentFirst = new JmxAgent();
        // If you specified "JmxAgent", it was the first one in the map,
        // but for "jmxAgent" the order was not preserved.
        // MX4JAgent depends on JmxAgent having finished initilisation
        // before proceeding, otherwise it is not able to find any
        // MBeanServer.
        agentFirst.setName("jmxAgent");
        MuleManager.getRegistry().registerAgent(agentFirst);

        Mx4jAgent agentSecond = new Mx4jAgent();
        agentSecond.setName("mx4jAgent");
        MuleManager.getRegistry().registerAgent(agentSecond);

        manager.start();

        // should not throw an exception
    }

    /**
     * Should not bark when the MBeanServer is injected and
     * {@code locateServer} and {@code createServer} both
     * set to false.
     */
    public void testJmxAgentInjectedMBeanServer() throws Exception
    {
        manager.setId("MuleAgentsTestCase.jmxAgentInjectedMBeanServer");
        JmxAgent jmxAgent = new JmxAgent();
        List servers = MBeanServerFactory.findMBeanServer(null);
        MBeanServer server = null;
        server = servers == null || servers.isEmpty()
                ? MBeanServerFactory.createMBeanServer()
                : (MBeanServer) servers.get(0);
        jmxAgent.setCreateServer(false);
        jmxAgent.setLocateServer(false);
        jmxAgent.setMBeanServer(server);
        MuleManager.getRegistry().registerAgent(jmxAgent);
        manager.start();
    }
}
