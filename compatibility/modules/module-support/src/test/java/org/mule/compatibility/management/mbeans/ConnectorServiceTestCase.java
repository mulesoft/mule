/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.management.mbeans;

import static org.junit.Assert.assertEquals;
import static org.mule.compatibility.core.registry.MuleRegistryTransportHelper.registerConnector;

import org.mule.compatibility.core.api.transport.Connector;
import org.mule.runtime.module.management.agent.JmxApplicationAgent;
import org.mule.runtime.module.management.agent.RmiRegistryAgent;
import org.mule.runtime.module.management.support.AutoDiscoveryJmxSupportFactory;
import org.mule.runtime.module.management.support.JmxSupport;
import org.mule.runtime.module.management.support.JmxSupportFactory;
import org.mule.tck.junit4.AbstractMuleContextEndpointTestCase;
import org.mule.tck.testmodels.mule.TestConnector;

import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.junit.Test;

public class ConnectorServiceTestCase extends AbstractMuleContextEndpointTestCase {

  protected String domainName;
  protected JmxApplicationAgent jmxAgent;

  protected MBeanServer mBeanServer;
  protected JmxSupportFactory jmxSupportFactory = AutoDiscoveryJmxSupportFactory.getInstance();
  protected JmxSupport jmxSupport = jmxSupportFactory.getJmxSupport();

  @Override
  protected void doSetUp() throws Exception {
    RmiRegistryAgent rmiRegistryAgent = new RmiRegistryAgent();
    rmiRegistryAgent.setMuleContext(muleContext);
    rmiRegistryAgent.initialise();
    muleContext.getRegistry().registerAgent(rmiRegistryAgent);

    mBeanServer = ManagementFactory.getPlatformMBeanServer();

    jmxAgent = muleContext.getRegistry().lookupObject(JmxApplicationAgent.class);
    domainName = jmxSupport.getDomainName(muleContext);
  }

  protected void unregisterMBeansByMask(final String mask) throws Exception {
    Set<ObjectInstance> objectInstances = mBeanServer.queryMBeans(jmxSupport.getObjectName(mask), null);
    for (ObjectInstance instance : objectInstances) {
      try {
        mBeanServer.unregisterMBean(instance.getObjectName());
      } catch (Exception e) {
        // ignore
      }
    }
  }

  @Override
  protected void doTearDown() throws Exception {
    unregisterMBeansByMask(domainName + ":*");
    unregisterMBeansByMask(domainName + ".1:*");
    unregisterMBeansByMask(domainName + ".2:*");
    mBeanServer = null;
  }

  @Test
  public void testDummy() {
    // this method only exists to silence the test runner
  }

  @Test
  public void testUndeploy() throws Exception {
    final Connector connector = new TestConnector(muleContext);
    connector.setName("TEST_CONNECTOR");
    registerConnector(muleContext.getRegistry(), connector);
    muleContext.start();

    final String query = domainName + ":*";
    final ObjectName objectName = jmxSupport.getObjectName(query);
    Set<ObjectInstance> mbeans = mBeanServer.queryMBeans(objectName, null);

    // Expecting following mbeans to be registered:
    // 1) org.mule.management.mbeans.StatisticsService@Mule.ConnectorServiceTest:type=org.mule.Statistics,name=AllStatistics
    // 2)
    // org.mule.management.mbeans.MuleConfigurationService@Mule.ConnectorServiceTest:type=org.mule.Configuration,name=GlobalConfiguration
    // 3) org.mule.management.mbeans.MuleService@Mule.ConnectorServiceTest:type=org.mule.MuleContext,name=MuleServerInfo
    // 4) org.mule.management.mbeans.ConnectorService@Mule.ConnectorServiceTest:type=org.mule.Connector,name="TEST.CONNECTOR"
    // 5) org.mule.module.management.mbean.ApplicationService:type=Application,name="totals for all flows and services"]
    // 6) org.mule.module.management.mbean.FlowConstructStats:type=org.mule.Statistics,Application=totals for all flows and
    // services]
    assertEquals("Unexpected number of components registered in the domain.", 6, mbeans.size());
    muleContext.dispose();

    mbeans = mBeanServer.queryMBeans(objectName, null);
    assertEquals("There should be no MBeans left in the domain", 0, mbeans.size());
  }
}
