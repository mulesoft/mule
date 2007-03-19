/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.agents;

import org.mule.tck.AbstractMuleTestCase;

import java.util.Map;

import javax.management.remote.rmi.RMIConnectorServer;

public class DefaultJmxSupportAgentTestCase extends AbstractMuleTestCase
{
    public void testHostPropertyEnablesClientSocketFactory () throws Exception
    {
        DefaultJmxSupportAgent agent = new DefaultJmxSupportAgent();
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
