/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import static org.mule.runtime.core.internal.construct.AbstractFlowConstruct.FLOW_FLOW_CONSTRUCT_TYPE;
import static org.mule.test.allure.AllureConstants.PricingMetricsFeature.PRICING_METRICS;
import static org.mule.test.allure.AllureConstants.PricingMetricsFeature.MessageMetricsStory.LAPSED_MESSAGE_METRICS;

import static java.util.Arrays.asList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.mule.runtime.core.api.management.stats.AllStatistics;
import org.mule.runtime.core.api.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.api.management.stats.ResetOnQueryCounter;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@SmallTest
@Feature(PRICING_METRICS)
@Story(LAPSED_MESSAGE_METRICS)
@RunWith(Parameterized.class)
public class ResetOnQueryCounterTestCase extends AbstractMuleContextTestCase {

  @Parameters(name = "{0}")
  public static List<Object[]> params() {
    return asList(
                  new Object[] {
                      "eventsReceived",
                      (Function<FlowConstructStatistics, ResetOnQueryCounter>) FlowConstructStatistics::getEventsReceivedCounter,
                      (Consumer<DefaultFlowConstructStatistics>) DefaultFlowConstructStatistics::incReceivedEvents},
                  new Object[] {
                      "dispatchedMessages",
                      (Function<FlowConstructStatistics, ResetOnQueryCounter>) FlowConstructStatistics::getDispatchedMessagesCounter,
                      (Consumer<DefaultFlowConstructStatistics>) DefaultFlowConstructStatistics::incMessagesDispatched},
                  new Object[] {
                      "executionErrors",
                      (Function<FlowConstructStatistics, ResetOnQueryCounter>) FlowConstructStatistics::getExecutionErrorsCounter,
                      (Consumer<DefaultFlowConstructStatistics>) DefaultFlowConstructStatistics::incExecutionError},
                  new Object[] {
                      "fatalErrors",
                      (Function<FlowConstructStatistics, ResetOnQueryCounter>) FlowConstructStatistics::getFatalErrorsCounter,
                      (Consumer<DefaultFlowConstructStatistics>) DefaultFlowConstructStatistics::incFatalError});
  }

  @Parameter(0)
  public String paramsConfigName;

  @Parameter(1)
  public Function<FlowConstructStatistics, ResetOnQueryCounter> createCounter;

  @Parameter(2)
  public Consumer<DefaultFlowConstructStatistics> incrementCounter;

  private DefaultFlowConstructStatistics flow1Stats;
  private DefaultFlowConstructStatistics flow2Stats;
  private AllStatistics allStatistics;

  @Before
  public void before() {
    flow1Stats = new DefaultFlowConstructStatistics(FLOW_FLOW_CONSTRUCT_TYPE, "someFlow1");
    flow2Stats = new DefaultFlowConstructStatistics(FLOW_FLOW_CONSTRUCT_TYPE, "someFlow2");

    allStatistics = new AllStatistics();
    allStatistics.add(flow1Stats);
    allStatistics.add(flow2Stats);
  }

  @Test
  public void flowCountersInitialState() {
    ResetOnQueryCounter beforeIncCounter = createCounter.apply(flow1Stats);
    incrementCounter.accept(flow1Stats);
    ResetOnQueryCounter afterIncCounter = createCounter.apply(flow1Stats);

    assertThat(beforeIncCounter.getAndReset(), is(1L));
    assertThat(afterIncCounter.getAndReset(), is(1L));
  }

  @Test
  public void flowCountersIndependentFromEachOther() {
    ResetOnQueryCounter counterA = createCounter.apply(flow1Stats);
    ResetOnQueryCounter counterB = createCounter.apply(flow1Stats);
    incrementCounter.accept(flow1Stats);

    assertThat(counterA.getAndReset(), is(1L));
    assertThat(counterA.get(), is(0L));
    assertThat(counterB.get(), is(1L));
  }

  @Test
  public void flowCountersUnaffectdByClear() {
    ResetOnQueryCounter counter = createCounter.apply(flow1Stats);

    incrementCounter.accept(flow1Stats);
    assertThat(counter.get(), is(1L));

    flow1Stats.clear();
    assertThat(counter.get(), is(1L));
  }

  @Test
  public void appLevelAggregationCountersInitialState() {
    ResetOnQueryCounter beforeIncEventsReceivedCounter = createCounter.apply(allStatistics.getApplicationStatistics());
    incrementCounter.accept(flow1Stats);
    ResetOnQueryCounter after1IncEventsReceivedCounter = createCounter.apply(allStatistics.getApplicationStatistics());
    incrementCounter.accept(flow2Stats);
    ResetOnQueryCounter after2IncEventsReceivedCounter = createCounter.apply(allStatistics.getApplicationStatistics());

    assertThat(beforeIncEventsReceivedCounter.getAndReset(), is(2L));
    assertThat(after1IncEventsReceivedCounter.getAndReset(), is(2L));
    assertThat(after2IncEventsReceivedCounter.getAndReset(), is(2L));
  }

  @Test
  public void appLevelAggregationCountersIndependentFromEachOther() {
    ResetOnQueryCounter counterA = createCounter.apply(allStatistics.getApplicationStatistics());
    ResetOnQueryCounter counterB = createCounter.apply(allStatistics.getApplicationStatistics());

    incrementCounter.accept(flow1Stats);

    assertThat(counterA.getAndReset(), is(1L));
    assertThat(counterA.get(), is(0L));
    assertThat(counterB.get(), is(1L));

    incrementCounter.accept(flow2Stats);

    assertThat(counterA.getAndReset(), is(1L));
    assertThat(counterA.get(), is(0L));
    assertThat(counterB.get(), is(2L));
  }

  @Test
  public void appLevelAggregationAndFlowCountersIndependentFromEachOther() {
    ResetOnQueryCounter flow1Counter = createCounter.apply(flow1Stats);
    ResetOnQueryCounter counter = createCounter.apply(allStatistics.getApplicationStatistics());

    incrementCounter.accept(flow1Stats);

    assertThat(flow1Counter.getAndReset(), is(1L));
    assertThat(flow1Counter.get(), is(0L));
    assertThat(counter.get(), is(1L));

    incrementCounter.accept(flow2Stats);

    assertThat(flow1Counter.get(), is(0L));
    assertThat(counter.get(), is(2L));
  }

  @Test
  public void appLevelAggregationCountersUnaffectdByClear() {
    ResetOnQueryCounter counter = createCounter.apply(allStatistics.getApplicationStatistics());

    incrementCounter.accept(flow1Stats);
    assertThat(counter.get(), is(1L));

    allStatistics.getApplicationStatistics().clear();
    assertThat(counter.get(), is(1L));

    incrementCounter.accept(flow2Stats);
    assertThat(counter.get(), is(2L));

    allStatistics.getApplicationStatistics().clear();
    assertThat(counter.get(), is(2L));
  }

  @Test
  public void appLevelAggregationCountersUnaffectdByFlowCountersClear() {
    ResetOnQueryCounter counter = createCounter.apply(allStatistics.getApplicationStatistics());

    incrementCounter.accept(flow1Stats);
    assertThat(counter.get(), is(1L));

    flow1Stats.clear();
    assertThat(counter.get(), is(1L));

    incrementCounter.accept(flow2Stats);
    assertThat(counter.get(), is(2L));

    flow2Stats.clear();
    assertThat(counter.get(), is(2L));
  }

  @Test
  public void flowCountersUnaffectdByAppLevelAggregationCountersClear() {
    ResetOnQueryCounter counter1 = createCounter.apply(flow1Stats);
    ResetOnQueryCounter counter2 = createCounter.apply(flow2Stats);

    incrementCounter.accept(flow1Stats);
    assertThat(counter1.get(), is(1L));
    assertThat(counter2.get(), is(0L));

    allStatistics.clear();
    assertThat(counter1.get(), is(1L));
    assertThat(counter2.get(), is(0L));

    incrementCounter.accept(flow2Stats);
    assertThat(counter1.get(), is(1L));
    assertThat(counter2.get(), is(1L));

    allStatistics.clear();
    assertThat(counter1.get(), is(1L));
    assertThat(counter2.get(), is(1L));
  }
}
