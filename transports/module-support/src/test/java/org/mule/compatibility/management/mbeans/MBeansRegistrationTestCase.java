/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.management.mbeans;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.compatibility.module.management.agent.TransportsJmxApplicationAgent;
import org.mule.compatibility.module.management.agent.mbean.ConnectorService;
import org.mule.compatibility.module.management.agent.mbean.EndpointService;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.module.management.support.JmxSupport;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;

import org.junit.Test;

/**
 * Verify that expected MBeans are registered based on the config.
 */
public class MBeansRegistrationTestCase extends FunctionalTestCase {

  private MBeanServer mBeanServer;
  private String domainName;
  private JmxSupport jmxSupport;

  @Override
  protected String getConfigFile() {
    return "mbeans-test-flow.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    TransportsJmxApplicationAgent jmxAgent =
        (TransportsJmxApplicationAgent) muleContext.getRegistry().lookupAgent("transports-jmx-agent");
    jmxSupport = jmxAgent.getJmxSupportFactory().getJmxSupport();
    domainName = jmxSupport.getDomainName(muleContext);
    mBeanServer = jmxAgent.getMBeanServer();
  }

  /**
   * Verify that all expected MBeans are registered for connectors, services, routers, and endpoints.
   */
  @Test
  public void testServiceMBeansRegistration() throws Exception {
    List<String> mbeanClasses = getMBeanClasses();

    assertTrue(mbeanClasses.contains(ConnectorService.class.getName()));
    assertTrue(mbeanClasses.contains(EndpointService.class.getName()));
  }

  /**
   * Verify that all MBeans were unregistered during disposal phase.
   */
  @Test
  public void testMBeansUnregistration() throws Exception {
    muleContext.dispose();
    assertEquals("No MBeans should be registered after disposal of MuleContext", 0, getMBeanClasses().size());
  }

  protected List<String> getMBeanClasses() throws MalformedObjectNameException {
    Set<ObjectInstance> mbeans = getMBeans();
    Iterator<ObjectInstance> it = mbeans.iterator();
    List<String> mbeanClasses = new ArrayList<String>();
    while (it.hasNext()) {
      mbeanClasses.add(it.next().getClassName());
    }
    return mbeanClasses;
  }

  protected Set<ObjectInstance> getMBeans() throws MalformedObjectNameException {
    return mBeanServer.queryMBeans(jmxSupport.getObjectName(domainName + ":*"), null);
  }
}
