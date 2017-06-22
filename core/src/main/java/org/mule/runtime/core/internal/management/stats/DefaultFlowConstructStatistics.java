/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import org.mule.runtime.core.api.management.stats.ComponentStatistics;

import java.util.concurrent.atomic.AtomicLong;

public class DefaultFlowConstructStatistics extends AbstractFlowConstructStatistics {

  private static final long serialVersionUID = 5337576392583767442L;
  private final AtomicLong executionError = new AtomicLong(0);
  private final AtomicLong fatalError = new AtomicLong(0);
  protected final ComponentStatistics flowStatistics = new ComponentStatistics();

  public DefaultFlowConstructStatistics(String flowConstructType, String name) {
    super(flowConstructType, name);
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
    executionError.addAndGet(1);
  }

  @Override
  public void incFatalError() {
    fatalError.addAndGet(1);
  }

  /**
   * Enable statistics logs (this is a dynamic parameter)
   */
  @Override
  public synchronized void setEnabled(boolean b) {
    super.setEnabled(b);
    flowStatistics.setEnabled(enabled);
  }

  @Override
  public synchronized void clear() {
    super.clear();

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

}
