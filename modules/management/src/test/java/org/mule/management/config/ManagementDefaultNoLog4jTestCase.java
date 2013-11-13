/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management.config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.api.agent.Agent;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class ManagementDefaultNoLog4jTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
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
