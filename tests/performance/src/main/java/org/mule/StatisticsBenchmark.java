/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static org.mule.runtime.core.internal.construct.AbstractFlowConstruct.FLOW_FLOW_CONSTRUCT_TYPE;

import static org.openjdk.jmh.annotations.Level.Trial;
import static org.openjdk.jmh.annotations.Scope.Benchmark;

import org.mule.runtime.core.api.management.stats.ResetOnQueryCounter;
import org.mule.runtime.core.internal.management.stats.DefaultFlowConstructStatistics;

import java.util.Collection;
import java.util.HashSet;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

public class StatisticsBenchmark extends AbstractBenchmark {

  @State(Benchmark)
  public static class MyState {

    public DefaultFlowConstructStatistics noCounters = new DefaultFlowConstructStatistics(FLOW_FLOW_CONSTRUCT_TYPE, "flow0");
    public DefaultFlowConstructStatistics oneCounter = new DefaultFlowConstructStatistics(FLOW_FLOW_CONSTRUCT_TYPE, "flow1");
    public DefaultFlowConstructStatistics fiveCounters = new DefaultFlowConstructStatistics(FLOW_FLOW_CONSTRUCT_TYPE, "flow5");

    private ResetOnQueryCounter oneEventsReceivedCounters;
    private Collection<ResetOnQueryCounter> fiveEventsReceivedCounters;

    @Setup(Trial)
    public void doSetup() {
      oneEventsReceivedCounters = oneCounter.getEventsReceivedCounter();

      fiveEventsReceivedCounters = new HashSet<>();
      for (int i = 0; i < 5; ++i) {
        fiveEventsReceivedCounters.add(fiveCounters.getEventsReceivedCounter());
      }
    }
  }

  @Benchmark
  public Object incrementValueNoCounters(MyState state) {
    state.noCounters.incReceivedEvents();
    return state.noCounters.getTotalEventsReceived();
  }

  @Benchmark
  public Object incrementValueOneCounter(MyState state) {
    state.oneCounter.incReceivedEvents();
    return state.oneCounter.getTotalEventsReceived();
  }

  @Benchmark
  public Object incrementValueFiveCounters(MyState state) {
    state.fiveCounters.incReceivedEvents();
    return state.fiveCounters.getTotalEventsReceived();
  }

}
