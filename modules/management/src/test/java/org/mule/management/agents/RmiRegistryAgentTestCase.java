/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.management.agents;

import org.mule.module.management.agent.RmiRegistryAgent;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RmiRegistryAgentTestCase extends AbstractMuleTestCase
{

    @Test
    public void testHostSetOnly() throws Exception
    {
        RmiRegistryAgent agent = new RmiRegistryAgent();
        agent.setHost("www.example.com");
        agent.initialise();
        assertEquals("rmi://www.example.com:1099", agent.getServerUri());
    }

    @Test
    public void testPortSetOnly() throws Exception
    {
        RmiRegistryAgent agent = new RmiRegistryAgent();
        agent.setPort("1095");
        agent.initialise();
        assertEquals("rmi://localhost:1095", agent.getServerUri());
    }

    @Test
    public void testHostAndPortSet() throws Exception
    {
        RmiRegistryAgent agent = new RmiRegistryAgent();
        agent.setPort("1095");
        agent.setHost("www.example.com");
        agent.initialise();
        assertEquals("rmi://www.example.com:1095", agent.getServerUri());
    }
  
    @Test
    public void testStart() throws Exception
    {
        RmiRegistryAgent agent = new RmiRegistryAgent();
        agent.initialise();
        agent.start();
    }

}
