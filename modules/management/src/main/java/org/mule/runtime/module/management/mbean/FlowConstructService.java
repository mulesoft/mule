/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.management.mbean;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.construct.AbstractFlowConstruct;
import org.mule.runtime.core.management.stats.FlowConstructStatistics;
import org.mule.runtime.module.management.support.AutoDiscoveryJmxSupportFactory;
import org.mule.runtime.module.management.support.JmxSupport;
import org.mule.runtime.module.management.support.JmxSupportFactory;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>FlowConstructService</code> exposes service information about a Mule Managed flow construct.
 */
public class FlowConstructService implements FlowConstructServiceMBean, MBeanRegistration, FlowConstructStatsMBean {

  private static Logger LOGGER = LoggerFactory.getLogger(FlowConstructService.class);

  protected FlowConstructStatistics statistics;

  protected MBeanServer server;

  protected String name;

  protected String type;

  protected ObjectName statsName;

  protected ObjectName objectName;

  protected MuleContext muleContext;

  // JmxSupport in order to build MBean's ObjectNames properly.
  protected JmxSupportFactory jmxSupportFactory = AutoDiscoveryJmxSupportFactory.getInstance();
  protected JmxSupport jmxSupport = jmxSupportFactory.getJmxSupport();

  public FlowConstructService(String type, String name, MuleContext muleContext, FlowConstructStatistics statistics) {
    this.muleContext = muleContext;
    this.type = type;
    this.name = name;
    this.statistics = statistics;
  }

  protected FlowConstructService(String type, String name, MuleContext muleContext) {
    this.muleContext = muleContext;
    this.type = type;
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public ObjectName getStatistics() {
    return statsName;
  }

  @Override
  public void clearStatistics() {
    statistics.clear();
  }

  @Override
  public long getTotalEventsReceived() {
    return statistics.getTotalEventsReceived();
  }

  @Override
  public long getAverageProcessingTime() {
    return statistics.getAverageProcessingTime();
  }

  @Override
  public long getProcessedEvents() {
    return statistics.getProcessedEvents();
  }

  @Override
  public long getMaxProcessingTime() {
    return statistics.getMaxProcessingTime();
  }

  @Override
  public long getMinProcessingTime() {
    return statistics.getMinProcessingTime();
  }

  @Override
  public long getTotalProcessingTime() {
    return statistics.getTotalProcessingTime();
  }

  @Override
  public long getExecutionErrors() {
    return statistics.getExecutionErrors();
  }

  @Override
  public long getFatalErrors() {
    return statistics.getFatalErrors();
  }

  @Override
  public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
    this.server = server;
    this.objectName = name;
    return name;
  }

  @Override
  public void postRegister(Boolean registrationDone) {
    AbstractFlowConstruct flow = muleContext.getRegistry().lookupObject(getName());
    try {
      if (flow.getStatistics() != null) {
        statsName = jmxSupport.getObjectName(String.format("%s:type=org.mule.Statistics,%s=%s", objectName.getDomain(),
                                                           flow.getConstructType(), jmxSupport.escape(getName())));

        // unregister old version if exists
        if (this.server.isRegistered(statsName)) {
          this.server.unregisterMBean(statsName);
        }

        this.server.registerMBean(new FlowConstructStats(flow.getStatistics()), this.statsName);
      }
    } catch (Exception e) {
      LOGGER.error("Error post-registering the MBean", e);
    }
  }

  @Override
  public void preDeregister() throws Exception {
    try {
      if (this.server.isRegistered(statsName)) {
        this.server.unregisterMBean(statsName);
      }
    } catch (Exception ex) {
      LOGGER.error("Error unregistering ServiceService child " + statsName.getCanonicalName(), ex);
    }
  }

  @Override
  public void postDeregister() {
    // nothing to do
  }
}
