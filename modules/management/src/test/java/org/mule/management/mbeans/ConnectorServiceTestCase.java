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

import org.mule.api.transport.Connector;
import org.mule.management.AbstractMuleJmxTestCase;
import org.mule.module.management.agent.JmxAgent;
import org.mule.tck.testmodels.mule.TestConnector;

import java.util.Set;

import javax.management.ObjectName;

public class ConnectorServiceTestCase extends AbstractMuleJmxTestCase
{

    public void testUndeploy() throws Exception
    {
        final String configId = "ConnectorServiceTest";
        muleContext.getConfiguration().setId(configId);
        final Connector connector = new TestConnector();
        connector.setName("TEST_CONNECTOR");
        final JmxAgent jmxAgent = new JmxAgent();
        muleContext.getRegistry().registerConnector(connector);
        muleContext.getRegistry().registerAgent(jmxAgent);
        muleContext.start();

        final String query = jmxSupport.getDomainName(muleContext) + ":*";
        final ObjectName objectName = jmxSupport.getObjectName(query);
        Set mbeans = mBeanServer.queryMBeans(objectName, null);

        // Expecting following mbeans to be registered:
        // 1) org.mule.management.mbeans.StatisticsService@Mule.ConnectorServiceTest:type=org.mule.Statistics,name=AllStatistics
        // 2) org.mule.management.mbeans.MuleConfigurationService@Mule.ConnectorServiceTest:type=org.mule.Configuration,name=GlobalConfiguration
        // 3) org.mule.management.mbeans.ModelService@Mule.ConnectorServiceTest:type=org.mule.Model,name="_muleSystemModel(seda)"
        // 4) org.mule.management.mbeans.MuleService@Mule.ConnectorServiceTest:type=org.mule.MuleContext,name=MuleServerInfo
        // 5) org.mule.management.mbeans.ConnectorService@Mule.ConnectorServiceTest:type=org.mule.Connector,name="TEST.CONNECTOR"
        assertEquals("Unexpected number of components registered in the domain.", 5, mbeans.size());
        muleContext.dispose();

        mbeans = mBeanServer.queryMBeans(objectName, null);
        assertEquals("There should be no MBeans left in the domain", 0, mbeans.size());
    }
}
