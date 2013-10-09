/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.management;

import org.mule.api.MuleException;
import org.mule.module.management.agent.JmxAgent;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ManagementStartupTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/management/management-startup-test.xml";
    }

    @Test
    public void testAgentConfiguration() throws MuleException
    {
        JmxAgent agent = muleContext.getRegistry().lookupObject(JmxAgent.class);
        assertNotNull(agent);
        assertNotNull(agent.getConnectorServerUrl());
        assertEquals("service:jmx:rmi:///jndi/rmi://0.0.0.0:1100/server", agent.getConnectorServerUrl());
        assertNotNull(agent.getConnectorServerProperties());
        assertEquals("true", agent.getConnectorServerProperties().get("jmx.remote.jndi.rebind"));
    }

}
