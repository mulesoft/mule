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
import org.mule.module.management.agent.ConfigurableJMXAuthenticator;
import org.mule.module.management.agent.JmxAgent;
import org.mule.tck.FunctionalTestCase;

import java.util.Map;

import javax.security.auth.Subject;

public class ManagementCustomJMXAuthenticatorTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "management-custom-jmx-authenticator-config.xml";
    }

    public void testCustomJMXAuthenticatorConfig() throws Exception
    {
        Agent agent = muleContext.getRegistry().lookupAgent("jmx-agent");
        assertNotNull(agent);
        assertEquals(JmxAgent.class, agent.getClass());
        JmxAgent jmxAgent = (JmxAgent) agent;
        assertEquals(CustomJMXAuthenticator.class, jmxAgent.getJmxAuthenticator().getClass());
    }

    public static class CustomJMXAuthenticator implements ConfigurableJMXAuthenticator
    {

        public void configure(Map credentials)
        {
        }

        public Subject authenticate(Object credentials)
        {
            return null;
        }
    }
}
