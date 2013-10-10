/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
