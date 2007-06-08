/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.mbeans;

import org.mule.management.AbstractMuleJmxTestCase;
import org.mule.management.agents.JmxAgent;
import org.mule.umo.provider.UMOConnector;

import java.util.Set;

import javax.management.ObjectName;

public class ConnectorServiceTestCase extends AbstractMuleJmxTestCase
{
    public void testUndeploy() throws Exception
    {
        final String configId = "ConnectorServiceTest";
        managementContext.setId(configId);
        final UMOConnector connector = getTestConnector();
        connector.setName("TEST_CONNECTOR");
        final JmxAgent jmxAgent = new JmxAgent();
        managementContext.getRegistry().registerConnector(connector);
        managementContext.getRegistry().registerAgent(jmxAgent);
        managementContext.start();

        final String query = jmxSupport.getDomainName(managementContext) + ":*";
        final ObjectName objectName = jmxSupport.getObjectName(query);
        Set mbeans = mBeanServer.queryMBeans(objectName, null);
        assertEquals("Unexpected number of components registered in the domain.", 6, mbeans.size());

        managementContext.dispose();

        mbeans = mBeanServer.queryMBeans(objectName, null);
        assertEquals("There should be no MBeans left in the domain", 0, mbeans.size());
    }
}
