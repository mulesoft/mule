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

import org.mule.management.agents.JmxAgent;
import org.mule.management.AbstractMuleJmxTestCase;
import org.mule.umo.manager.UMOManager;
import org.mule.umo.provider.UMOConnector;

import javax.management.ObjectName;

import java.util.Set;

public class ConnectorServiceTestCase extends AbstractMuleJmxTestCase
{
    public void testUndeploy() throws Exception
    {
        final UMOManager manager = getManager(true);
        final UMOConnector connector = getTestConnector();
        connector.setName("TEST_CONNECTOR");
        final JmxAgent jmxAgent = new JmxAgent();
        manager.registerConnector(connector);
        manager.registerAgent(jmxAgent);
        manager.start();

        Set mbeans = mBeanServer.queryMBeans(ObjectName.getInstance("Mule:*"), null);
        assertEquals("Unexpected number of components registered in the domain.", 5, mbeans.size());

        manager.dispose();

        mbeans = mBeanServer.queryMBeans(ObjectName.getInstance("Mule:*"), null);
        assertEquals("There should be no MBeans left in the domain", 0, mbeans.size());
    }
}
