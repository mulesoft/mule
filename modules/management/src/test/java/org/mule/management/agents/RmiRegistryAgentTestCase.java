/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
