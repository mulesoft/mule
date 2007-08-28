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
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.manager.UMOAgent;

public class ManagementNamespaceHandlerTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "management-namespace-config.xml";
    }
    

    public void testSimpleJmxAgentConfig() throws Exception
    {
        UMOAgent agent = managementContext.getRegistry().lookupAgent("simpleJmxServer");
        assertNotNull(agent);
        assertEquals( JmxAgent.class, agent.getClass());
        JmxAgent jmxAgent=(JmxAgent) agent;
        assertEquals(true, jmxAgent.isCreateServer());
        assertEquals(true, jmxAgent.isLocateServer());
        assertEquals(true, jmxAgent.isEnableStatistics());
    }

    
    

}


