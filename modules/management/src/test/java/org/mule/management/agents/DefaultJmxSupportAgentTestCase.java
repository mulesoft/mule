/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.management.agents;

import org.mule.module.management.agent.DefaultJmxSupportAgent;
import org.mule.module.management.agent.FixedHostRmiClientSocketFactory;
import org.mule.module.management.agent.JmxAgent;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Map;

import javax.management.remote.rmi.RMIConnectorServer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DefaultJmxSupportAgentTestCase extends AbstractMuleContextTestCase
{
    @Test
    public void testHostPropertyEnablesClientSocketFactory () throws Exception
    {
        DefaultJmxSupportAgent agent = new DefaultJmxSupportAgent();
        agent.setMuleContext(muleContext);
        agent.setHost("127.0.0.1");
        JmxAgent jmxAgent = agent.createJmxAgent();
        Map props = jmxAgent.getConnectorServerProperties();
        assertNotNull(props);
        assertEquals("JMX ConnectorServer properties should've been merged",
                     2, props.size());
        assertTrue("Property shouldn't have been removed",
                   props.containsKey(RMIConnectorServer.JNDI_REBIND_ATTRIBUTE));
        assertTrue("Property should've been added",
                   props.containsKey(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE));
        Object ref = props.get(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE);
        assertNotNull(ref);
        assertTrue(ref instanceof FixedHostRmiClientSocketFactory);
    }
}
