/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.management.config;

import org.mule.api.agent.Agent;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ManagementDefaultNoLog4jTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "management-default-no-log4j-config.xml";
    }

    @Test
    public void testDefaultJmxAgentConfig() throws Exception
    {
        Agent agent = muleContext.getRegistry().lookupAgent("jmx-agent");
        assertNotNull(agent);

        agent = muleContext.getRegistry().lookupAgent("jmx-log4j");
        assertNull(agent);
    }

}
