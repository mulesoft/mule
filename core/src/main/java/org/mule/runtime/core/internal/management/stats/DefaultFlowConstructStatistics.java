/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import static java.lang.System.currentTimeMillis;

import org.mule.runtime.core.api.management.stats.ComponentStatistics;
import org.mule.runtime.core.api.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.api.management.stats.ResetOnQueryCounter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class DefaultFlowConstructStatistics implements FlowConstructStatistics {

  private static final long serialVersionUID = 5337576392583767442L;

  protected final String flowConstructType;
  protected String name;
  protected boolean enabled = false;
  private long samplePeriod = 0;
  protected final AtomicLong receivedEvents = new AtomicLong(0);
  protected transient final AtomicLong dispatchedMessages = new AtomicLong(0);

  private final AtomicLong executionError = new AtomicLong(0);
  private final AtomicLong fatalError = new AtomicLong(0);
  protected final ComponentStatistics flowStatistics = new ComponentStatistics();

  // Transient to avoid de-serialization backward compatibility problems (MULE-19020)
  private transient final AtomicLong connectionErrors = new AtomicLong(0);

  private transient final List<DefaultResetOnQueryCounter> eventsReceivedCounters = new CopyOnWriteArrayList<>();
  private transient final List<DefaultResetOnQueryCounter> messagesDispatchedCounters = new CopyOnWriteArrayList<>();
  private transient final List<DefaultResetOnQueryCounter> executionErrorsCounters = new CopyOnWriteArrayList<>();
  private transient final List<DefaultResetOnQueryCounter> connectionErrorsCounters = new CopyOnWriteArrayList<>();
  private transient final List<DefaultResetOnQueryCounter> fatalErrorsCounters = new CopyOnWriteArrayList<>();

  public DefaultFlowConstructStatistics(String flowConstructType, String name) {
    this.name = name;
    this.flowConstructType = flowConstructType;
    flowStatistics.setEnabled(enabled);
    if (this.getClass() == DefaultFlowConstructStatistics.class) {
      clear();
    }
  }

  /**
   * Are statistics logged
   */
  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void incExecutionError() {
    if (isEnabled()) {
      executionError.addAndGet(1);
      executionErrorsCounters.forEach(DefaultResetOnQueryCounter::increment);
    }
  }

  @Override
  public void incFatalError() {
    if (isEnabled()) {
      fatalError.addAndGet(1);
      fatalErrorsCounters.forEach(DefaultResetOnQueryCounter::increment);
    }
  }

  /**
   * Enable statistics logs (this is a dynamic parameter)
   */
  @Override
  public synchronized void setEnabled(boolean b) {
    enabled = b;
    flowStatistics.setEnabled(enabled);
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
    samplePeriod = currentTimeMillis();

    executionError.set(0);
    fatalError.set(0);
    if (flowStatistics != null) {
      flowStatistics.clear();
    }
  }

  @Override
  public void addCompleteFlowExecutionTime(long time) {
    flowStatistics.addCompleteExecutionTime(time);
  }

  @Override
  public void addFlowExecutionBranchTime(long time, long total) {
    flowStatistics.addExecutionBranchTime(time == total, time, total);
  }

  @Override
  public long getAverageProcessingTime() {
    return flowStatistics.getAverageExecutionTime();
  }

  @Override
  public long getProcessedEvents() {
    return flowStatistics.getExecutedEvents();
  }

  @Override
  public long getMaxProcessingTime() {
    return flowStatistics.getMaxExecutionTime();
  }

  @Override
  public long getMinProcessingTime() {
    return flowStatistics.getMinExecutionTime();
  }

  @Override
  public long getTotalProcessingTime() {
    return flowStatistics.getTotalExecutionTime();
  }

  @Override
  public long getExecutionErrors() {
    return executionError.get();
  }

  @Override
  public long getFatalErrors() {
    return fatalError.get();
  }

  @Override
  public long getConnectionErrors() {
    return connectionErrors.get();
  }

  @Override
  public void incReceivedEvents() {
    if (isEnabled()) {
      receivedEvents.addAndGet(1);
      eventsReceivedCounters.forEach(DefaultResetOnQueryCounter::increment);
    }
  }

  @Override
  public void incMessagesDispatched() {
    if (isEnabled()) {
      dispatchedMessages.addAndGet(1);
      messagesDispatchedCounters.forEach(DefaultResetOnQueryCounter::increment);
    }
  }

  @Override
  public void incConnectionErrors() {
    if (isEnabled()) {
      connectionErrors.addAndGet(1);
      connectionErrorsCounters.forEach(DefaultResetOnQueryCounter::increment);
    }
  }

  @Override
  public long getTotalEventsReceived() {
    return receivedEvents.get();
  }

  @Override
  public long getTotalDispatchedMessages() {
    return dispatchedMessages.get();
  }

  public long getSamplePeriod() {
    return currentTimeMillis() - samplePeriod;
  }

  @Override
  public ResetOnQueryCounter getEventsReceivedCounter() {
    DefaultResetOnQueryCounter counter = new DefaultResetOnQueryCounter();
    eventsReceivedCounters.add(counter);
    counter.add(getTotalEventsReceived());
    return counter;
  }

  @Override
  public ResetOnQueryCounter getDispatchedMessagesCounter() {
    DefaultResetOnQueryCounter counter = new DefaultResetOnQueryCounter();
    messagesDispatchedCounters.add(counter);
    counter.add(getTotalDispatchedMessages());
    return counter;
  }

  @Override
  public ResetOnQueryCounter getExecutionErrorsCounter() {
    DefaultResetOnQueryCounter counter = new DefaultResetOnQueryCounter();
    executionErrorsCounters.add(counter);
    counter.add(getExecutionErrors());
    return counter;
  }

  @Override
  public ResetOnQueryCounter getConnectionErrorsCounter() {
    DefaultResetOnQueryCounter counter = new DefaultResetOnQueryCounter();
    connectionErrorsCounters.add(counter);
    counter.add(getConnectionErrors());
    return counter;
  }

  @Override
  public ResetOnQueryCounter getFatalErrorsCounter() {
    DefaultResetOnQueryCounter counter = new DefaultResetOnQueryCounter();
    fatalErrorsCounters.add(counter);
    counter.add(getFatalErrors());
    return counter;
  }
}
