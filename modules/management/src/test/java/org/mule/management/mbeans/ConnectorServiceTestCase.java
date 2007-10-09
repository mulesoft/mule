/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.mbeans;

import org.mule.management.AbstractMuleJmxTestCase;
import org.mule.management.agents.JmxAgent;
import org.mule.umo.provider.UMOConnector;
import org.mule.RegistryContext;

import java.util.Set;

import javax.management.ObjectName;

public class ConnectorServiceTestCase extends AbstractMuleJmxTestCase
{
    // TODO Why 5?  Document the magic number!
    private static final int NUMBER_OF_COMPONENTS = 5;

    public void testUndeploy() throws Exception
    {
        final String configId = "ConnectorServiceTest";
        managementContext.setId(configId);
        final UMOConnector connector = getTestConnector();
        connector.setName("TEST_CONNECTOR");
        final JmxAgent jmxAgent = new JmxAgent();
        RegistryContext.getRegistry().registerConnector(connector, managementContext);
        RegistryContext.getRegistry().registerAgent(jmxAgent, managementContext);
        managementContext.start();

        final String query = jmxSupport.getDomainName(managementContext) + ":*";
        final ObjectName objectName = jmxSupport.getObjectName(query);
        Set mbeans = mBeanServer.queryMBeans(objectName, null);
        assertEquals("Unexpected number of components registered in the domain.", NUMBER_OF_COMPONENTS, mbeans.size());

        managementContext.dispose();
        //TODO: remove next line when MULE-2275 will be fixed.
        jmxAgent.dispose();

        mbeans = mBeanServer.queryMBeans(objectName, null);
        assertEquals("There should be no MBeans left in the domain", 0, mbeans.size());
    }
}
