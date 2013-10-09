/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.management.config;

import org.mule.api.agent.Agent;
import org.mule.module.management.agent.ConfigurableJMXAuthenticator;
import org.mule.module.management.agent.JmxAgent;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.Map;

import javax.security.auth.Subject;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ManagementCustomJMXAuthenticatorTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "management-custom-jmx-authenticator-config.xml";
    }

    @Test
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
