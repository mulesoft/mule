/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.management.mbean;

import org.mule.runtime.core.management.stats.FlowConstructStatistics;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * A concrete class that holds management information for a Mule managed flow.
 */
public class FlowConstructStats implements FlowConstructStatsMBean, MBeanRegistration {

  private final FlowConstructStatistics statistics;


  protected MBeanServer server;

  protected ObjectName name;

  public FlowConstructStats(FlowConstructStatistics statistics) {
    this.statistics = statistics;
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
  public void clearStatistics() {
    statistics.clear();
  }

  @Override
  public long getTotalEventsReceived() {
    return statistics.getTotalEventsReceived();
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
    this.name = name;
    return name;
  }

  @Override
  public void postRegister(Boolean registrationDone) {
    // nothing to do
  }

  @Override
  public void preDeregister() throws Exception {
    // nothing to do
  }

  @Override
  public void postDeregister() {
    // nothing to do
  }
}
