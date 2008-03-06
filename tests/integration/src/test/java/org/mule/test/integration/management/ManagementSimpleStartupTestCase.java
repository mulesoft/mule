/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.management;

import org.mule.api.MuleException;
import org.mule.management.agents.DefaultJmxSupportAgent;
import org.mule.tck.FunctionalTestCase;

public class ManagementSimpleStartupTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/integration/management/management-simple-startup-test.xml";
    }

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