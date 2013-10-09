/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.management;

import org.mule.api.MuleException;
import org.mule.module.management.agent.DefaultJmxSupportAgent;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ManagementSimpleStartupTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/management/management-simple-startup-test.xml";
    }

    @Test
    public void testAgentConfiguration() throws MuleException
    {
        DefaultJmxSupportAgent agent = (DefaultJmxSupportAgent) muleContext.getRegistry().lookupAgent("jmx-default-config");
        assertNotNull(agent);
        // these values are different from DEFAULT_HOST and DEFAULT_PORT in agent
        assertNotNull(agent.getHost());
        assertEquals("0.0.0.0", agent.getHost());
        assertNotNull(agent.getPort());
        assertEquals("1100", agent.getPort());
    }

}
