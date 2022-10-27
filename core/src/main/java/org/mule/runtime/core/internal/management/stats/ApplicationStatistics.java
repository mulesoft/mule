/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import org.mule.runtime.core.api.management.stats.AllStatistics;
import org.mule.runtime.core.api.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.api.management.stats.ResetOnQueryCounter;

import java.util.HashSet;
import java.util.Set;

/**
 * Aggregate statistics for all services and flows in an application. Do this by looping through all of the applications'
 * FlowConstructStatistics that aren't themselves aggregators.
 */
public class ApplicationStatistics extends DefaultFlowConstructStatistics {

  private final AllStatistics parent;

  public ApplicationStatistics(AllStatistics parent) {
    super("Application", "application totals");
    this.parent = parent;
  }

  @Override
  public long getAverageProcessingTime() {
    long totalTime = 0;
    long totalEvents = 0;
    for (FlowConstructStatistics stats : parent.getServiceStatistics()) {
      if (!(stats instanceof ApplicationStatistics)) {
        totalEvents += stats.getProcessedEvents();
        totalTime += stats.getTotalProcessingTime();
      }
    }
    return totalEvents == 0 ? 0 : totalTime / totalEvents;
  }

  @Override
  public long getProcessedEvents() {
    long total = 0;
    for (FlowConstructStatistics stats : parent.getServiceStatistics()) {
      if (!(stats instanceof ApplicationStatistics)) {
        total += stats.getProcessedEvents();
      }
    }
    return total;
  }

  @Override
  public long getMinProcessingTime() {
    long min = 0;
    boolean first = true;
    for (FlowConstructStatistics stats : parent.getServiceStatistics()) {
      if (!(stats instanceof ApplicationStatistics)) {
        long flowMin = stats.getMinProcessingTime();
        if (first) {
          min = flowMin;
        } else {
          min = Math.min(min, flowMin);
        }
      }
      first = false;
    }
    return min;
  }

  @Override
  public long getMaxProcessingTime() {
    long max = 0;
    for (FlowConstructStatistics stats : parent.getServiceStatistics()) {
      if (!(stats instanceof ApplicationStatistics)) {
        max = Math.max(max, stats.getMaxProcessingTime());
      }
    }
    return max;
  }

  @Override
  public long getTotalProcessingTime() {
    long total = 0;
    for (FlowConstructStatistics stats : parent.getServiceStatistics()) {
      if (!(stats instanceof ApplicationStatistics)) {
        total += stats.getTotalProcessingTime();
      }
    }
    return total;
  }

  @Override
  public long getExecutionErrors() {
    long total = 0;
    for (FlowConstructStatistics stats : parent.getServiceStatistics()) {
      if (!(stats instanceof ApplicationStatistics)) {
        total += stats.getExecutionErrors();
      }
    }
    return total;
  }

  @Override
  public long getFatalErrors() {
    long total = 0;
    for (FlowConstructStatistics stats : parent.getServiceStatistics()) {
      if (!(stats instanceof ApplicationStatistics)) {
        total += stats.getFatalErrors();
      }
    }
    return total;
  }

  @Override
  public long getTotalEventsReceived() {
    long total = 0;
    for (FlowConstructStatistics stats : parent.getServiceStatistics()) {
      if (!(stats instanceof ApplicationStatistics)) {
        total += stats.getTotalEventsReceived();
      }
    }
    return total;
  }

  public long getTotalDispatchedMessages() {
    long total = 0;
    for (FlowConstructStatistics stats : parent.getServiceStatistics()) {
      if (!(stats instanceof ApplicationStatistics)) {
        total += stats.getTotalDispatchedMessages();
      }
    }
    return total;
  }

  @Override
  public ResetOnQueryCounter getEventsReceivedCounter() {
    Set<ResetOnQueryCounter> counters = new HashSet<>();

    for (FlowConstructStatistics stats : parent.getServiceStatistics()) {
      if (!(stats instanceof ApplicationStatistics)) {
        counters.add(stats.getEventsReceivedCounter());
      }
    }

    return new CompositeResetOnQueryCounter(counters);
  }

  @Override
  public ResetOnQueryCounter getDispatchedMessagesCounter() {
    Set<ResetOnQueryCounter> counters = new HashSet<>();

    for (FlowConstructStatistics stats : parent.getServiceStatistics()) {
      if (!(stats instanceof ApplicationStatistics)) {
        counters.add(stats.getDispatchedMessagesCounter());
      }
    }

    return new CompositeResetOnQueryCounter(counters);
  }

  @Override
  public ResetOnQueryCounter getExecutionErrorsCounter() {
    Set<ResetOnQueryCounter> counters = new HashSet<>();

    for (FlowConstructStatistics stats : parent.getServiceStatistics()) {
      if (!(stats instanceof ApplicationStatistics)) {
        counters.add(stats.getExecutionErrorsCounter());
      }
    }

    return new CompositeResetOnQueryCounter(counters);
  }

  @Override
  public ResetOnQueryCounter getFatalErrorsCounter() {
    Set<ResetOnQueryCounter> counters = new HashSet<>();

    for (FlowConstructStatistics stats : parent.getServiceStatistics()) {
      if (!(stats instanceof ApplicationStatistics)) {
        counters.add(stats.getFatalErrorsCounter());
      }
    }

    return new CompositeResetOnQueryCounter(counters);
  }
}
