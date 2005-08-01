/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.management.mbeans;

import org.mule.config.MuleProperties;
import org.mule.management.agents.JmxAgent;
import org.mule.tck.AbstractMuleJmxTestCase;
import org.mule.umo.manager.UMOManager;
import org.mule.umo.provider.UMOConnector;

import javax.management.ObjectName;
import java.util.Set;

/**
 * @author <a href="mailto:aperepel@itci.com">Andrew Perepelytsya</a>
 *
 * $Id$$
 */
public class ConnectorServiceTestCase extends AbstractMuleJmxTestCase
{
    public void testUndeploy() throws Exception
    {
        final String domainOriginal = "TEST_DOMAIN_1";

        System.setProperty(MuleProperties.DISABLE_SERVER_CONNECTIONS, "true");
        final UMOManager manager = getManager();
        final UMOConnector connector = getTestConnector();
        connector.setName("TEST_CONNECTOR");
        final JmxAgent jmxAgent = new JmxAgent();
        jmxAgent.setDomain(domainOriginal);
        manager.registerConnector(connector);
        manager.registerAgent(jmxAgent);

        manager.start();
        System.setProperty(MuleProperties.DISABLE_SERVER_CONNECTIONS, "false");

        Set mbeans = mBeanServer.queryMBeans(ObjectName.getInstance(domainOriginal + ":*"), null);
        assertEquals("Unexpected number of components registered in the domain.", 4, mbeans.size());

        manager.dispose();

        mbeans = mBeanServer.queryMBeans(ObjectName.getInstance(domainOriginal + ":*"), null);
        assertEquals("There should be no MBeans left in the domain", 0, mbeans.size());
    }
}
