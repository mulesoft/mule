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
        managementContext.getRegistry().registerConnector(connector, managementContext);
        managementContext.getRegistry().registerAgent(jmxAgent, managementContext);


        managementContext.applyLifecycle(jmxAgent);

        managementContext.start();

        final String query = jmxSupport.getDomainName(managementContext) + ":*";
        final ObjectName objectName = jmxSupport.getObjectName(query);
        Set mbeans = mBeanServer.queryMBeans(objectName, null);

        // Expecting following mbeans to be registered:
        // 1) org.mule.management.mbeans.StatisticsService@Mule.ConnectorServiceTest:type=org.mule.Statistics,name=AllStatistics
        // 2) org.mule.management.mbeans.MuleConfigurationService@Mule.ConnectorServiceTest:type=org.mule.Configuration,name=GlobalConfiguration
        // 3) org.mule.management.mbeans.ModelService@Mule.ConnectorServiceTest:type=org.mule.Model,name="_muleSystemModel(seda)"
        // 4) org.mule.management.mbeans.MuleService@Mule.ConnectorServiceTest:type=org.mule.ManagementContext,name=MuleServerInfo
        // 5) org.mule.management.mbeans.ConnectorService@Mule.ConnectorServiceTest:type=org.mule.Connector,name="TEST.CONNECTOR"
        assertEquals("Unexpected number of components registered in the domain.", 5, mbeans.size());

        managementContext.dispose();
        //TODO: remove next line when MULE-2275 will be fixed.
        jmxAgent.dispose();

        mbeans = mBeanServer.queryMBeans(objectName, null);
        assertEquals("There should be no MBeans left in the domain", 0, mbeans.size());
    }
}
