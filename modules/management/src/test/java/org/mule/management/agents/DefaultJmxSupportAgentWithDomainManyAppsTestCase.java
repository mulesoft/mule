/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management.agents;

import static org.mule.management.agents.DefaultJmxSupportAgentTestCase.doTestHostPropertyEnablesClientSocketFactory;

import org.mule.tck.junit4.DomainFunctionalTestCase;

import org.junit.Test;

public class DefaultJmxSupportAgentWithDomainManyAppsTestCase extends DomainFunctionalTestCase
{

    @Override
    protected String getDomainConfig()
    {
        return "agent/empty-domain-config.xml";
    }

    @Override
    public ApplicationConfig[] getConfigResources()
    {
        return new ApplicationConfig[] {
                                        new ApplicationConfig("app1", new String[] {"agent/jmx-agent-app-config.xml"}),
                                        new ApplicationConfig("app2", new String[] {"agent/jmx-agent-app-config.xml"})
        };
    }

    @Test
    public void testHostPropertyEnablesClientSocketFactory () throws Exception
    {
        doTestHostPropertyEnablesClientSocketFactory(getMuleContextForApp("app1"));
    }
}
