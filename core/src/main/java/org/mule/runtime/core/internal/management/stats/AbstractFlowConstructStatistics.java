/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import org.mule.runtime.core.api.management.stats.FlowConstructStatistics;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Statistics common to flows and services
 */
public abstract class AbstractFlowConstructStatistics implements FlowConstructStatistics {

  private static final long serialVersionUID = 5337576392583767442L;

  protected final String flowConstructType;
  protected String name;
  protected boolean enabled = false;
  private long samplePeriod = 0;
  protected final AtomicLong receivedEvents = new AtomicLong(0);
  protected final AtomicLong dispatchedMessages = new AtomicLong(0);

  public AbstractFlowConstructStatistics(String flowConstructType, String name) {
    this.name = name;
    this.flowConstructType = flowConstructType;
  }

  /**
   * Enable statistics logs (this is a dynamic parameter)
   */
  @Override
  public synchronized void setEnabled(boolean b) {
    enabled = b;
  }

  /**
   * Are statistics logged
   */
  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public synchronized String getName() {
    return name;
  }

  public synchronized void setName(String name) {
    this.name = name;
  }

  @Override
  public synchronized void clear() {
    receivedEvents.set(0);
    dispatchedMessages.set(0);
    samplePeriod = System.currentTimeMillis();
  }


  @Override
  public void incReceivedEvents() {
    receivedEvents.addAndGet(1);
  }

  @Override
  public void incMessagesDispatched() {
    dispatchedMessages.addAndGet(1);
  }

  @Override
  public long getTotalEventsReceived() {
    return receivedEvents.get();
  }

  @Override
  public long getTotalDispatchedMessages() {
    return dispatchedMessages.get();
  }

  public String getFlowConstructType() {
    return flowConstructType;
  }

  public long getSamplePeriod() {
    return System.currentTimeMillis() - samplePeriod;
  }
}
