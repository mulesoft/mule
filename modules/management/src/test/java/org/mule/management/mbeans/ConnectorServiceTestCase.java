/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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

import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConnectorServiceTestCase extends AbstractMuleJmxTestCase
{

    protected String domainName;
    protected JmxAgent jmxAgent;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        jmxAgent = muleContext.getRegistry().lookupObject(JmxAgent.class);

    }

    @Test
    public void testUndeploy() throws Exception
    {
        final Connector connector = new TestConnector(muleContext);
        connector.setName("TEST_CONNECTOR");
        muleContext.getRegistry().registerConnector(connector);
        muleContext.start();

        domainName = jmxSupport.getDomainName(muleContext);
        final String query = domainName + ":*";
        final ObjectName objectName = jmxSupport.getObjectName(query);
        Set<ObjectInstance> mbeans = mBeanServer.queryMBeans(objectName, null);

        // Expecting following mbeans to be registered:
        // 1) org.mule.management.mbeans.StatisticsService@Mule.ConnectorServiceTest:type=org.mule.Statistics,name=AllStatistics
        // 2) org.mule.management.mbeans.MuleConfigurationService@Mule.ConnectorServiceTest:type=org.mule.Configuration,name=GlobalConfiguration
        // 3) org.mule.management.mbeans.ModelService@Mule.ConnectorServiceTest:type=org.mule.Model,name="_muleSystemModel(seda)"
        // 4) org.mule.management.mbeans.MuleService@Mule.ConnectorServiceTest:type=org.mule.MuleContext,name=MuleServerInfo
        // 5) org.mule.management.mbeans.ConnectorService@Mule.ConnectorServiceTest:type=org.mule.Connector,name="TEST.CONNECTOR"
        // 6) org.mule.module.management.mbean.ApplicationService:type=Application,name="totals for all flows and services"]
        // 7) org.mule.module.management.mbean.FlowConstructStats:type=org.mule.Statistics,Application=totals for all flows and services]
        assertEquals("Unexpected number of components registered in the domain.", 7, mbeans.size());
        muleContext.dispose();

        mbeans = mBeanServer.queryMBeans(objectName, null);
        assertEquals("There should be no MBeans left in the domain", 0, mbeans.size());
    }

    @Override
    protected void doTearDown() throws Exception
    {
        unregisterMBeansByMask(domainName + ":*");
    }
}
