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

import org.mule.api.agent.Agent;
import org.mule.tck.FunctionalTestCase;

public class ManagementDefaultNoLog4jTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "management-default-no-log4j-config.xml";
    }

    public void testDefaultJmxAgentConfig() throws Exception
    {
        Agent agent = muleContext.getRegistry().lookupAgent("jmx-agent");
        assertNotNull(agent);

        agent = muleContext.getRegistry().lookupAgent("jmx-log4j");
        assertNull(agent);
    }

}
