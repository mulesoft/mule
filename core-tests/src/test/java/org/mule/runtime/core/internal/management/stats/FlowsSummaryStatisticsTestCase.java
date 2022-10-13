/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import static org.mule.runtime.core.api.construct.Flow.INITIAL_STATE_STARTED;
import static org.mule.runtime.core.api.construct.Flow.INITIAL_STATE_STOPPED;
import static org.mule.test.allure.AllureConstants.PricingMetricsFeature.PRICING_METRICS;
import static org.mule.test.allure.AllureConstants.PricingMetricsFeature.FlowSummaryStory.ACTIVE_FLOWS_SUMMARY;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.deployment.management.ComponentInitialStateManager;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.api.management.stats.FlowsSummaryStatistics;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.construct.AbstractPipeline;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

import reactor.core.publisher.Mono;

/**
 * Note: flow-mappings are not considered
 *
 */
@SmallTest
@Feature(PRICING_METRICS)
@Story(ACTIVE_FLOWS_SUMMARY)
public class FlowsSummaryStatisticsTestCase extends AbstractMuleContextTestCase {

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();

    muleContext = spy(muleContext);
  }

  @Test
  public void triggerFlow() throws MuleException {
    DefaultFlowsSummaryStatistics flowsSummaryStatistics = new DefaultFlowsSummaryStatistics(true);
    TestPipeline flow = new TestPipeline("", muleContext,
                                         mock(MessageSource.class),
                                         emptyList(), empty(), empty(),
                                         INITIAL_STATE_STARTED,
                                         null,
                                         flowsSummaryStatistics,
                                         null, null);

    flow.initialise();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 0, 0, 0);

    flow.start();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 1, 0, 0);

    flow.stop();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 0, 0, 0);

    flow.dispose();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 0, 0, 0, 0);
  }

  @Test
  public void apikitFlow() throws MuleException {
    DefaultFlowsSummaryStatistics flowsSummaryStatistics = new DefaultFlowsSummaryStatistics(true);
    TestPipeline flow = new TestPipeline("get:\\reservation:api-config", muleContext,
                                         null,
                                         emptyList(), empty(), empty(),
                                         INITIAL_STATE_STARTED,
                                         null,
                                         flowsSummaryStatistics,
                                         null, null);

    flow.initialise();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 1, 0, 0, 0, 0);

    flow.start();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 1, 0, 0, 1, 0);

    flow.stop();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 1, 0, 0, 0, 0);

    flow.dispose();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 0, 0, 0, 0);
  }

  @Test
  public void apikitWithSourceFlow() throws MuleException {
    DefaultFlowsSummaryStatistics flowsSummaryStatistics = new DefaultFlowsSummaryStatistics(true);
    TestPipeline flow = new TestPipeline("get:\\reservation:api-config", muleContext,
                                         mock(MessageSource.class),
                                         emptyList(), empty(), empty(),
                                         INITIAL_STATE_STARTED,
                                         null,
                                         flowsSummaryStatistics,
                                         null, null);

    flow.initialise();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 0, 0, 0);

    flow.start();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 1, 0, 0);

    flow.stop();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 0, 0, 0);

    flow.dispose();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 0, 0, 0, 0);
  }

  @Test
  public void privateFlow() throws MuleException {
    DefaultFlowsSummaryStatistics flowsSummaryStatistics = new DefaultFlowsSummaryStatistics(true);
    TestPipeline flow = new TestPipeline("", muleContext,
                                         null,
                                         emptyList(), empty(), empty(),
                                         INITIAL_STATE_STARTED,
                                         null,
                                         flowsSummaryStatistics,
                                         null, null);

    flow.initialise();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 1, 0, 0, 0);

    flow.start();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 1, 0, 0, 1);

    flow.stop();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 1, 0, 0, 0);

    flow.dispose();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 0, 0, 0, 0);
  }

  @Test
  public void twoTriggerFlow() throws MuleException {
    DefaultFlowsSummaryStatistics flowsSummaryStatistics = new DefaultFlowsSummaryStatistics(true);
    TestPipeline flow1 = new TestPipeline("", muleContext,
                                          mock(MessageSource.class),
                                          emptyList(), empty(), empty(),
                                          INITIAL_STATE_STARTED,
                                          null,
                                          flowsSummaryStatistics,
                                          null, null);
    TestPipeline flow2 = new TestPipeline("", muleContext,
                                          mock(MessageSource.class),
                                          emptyList(), empty(), empty(),
                                          INITIAL_STATE_STARTED,
                                          null,
                                          flowsSummaryStatistics,
                                          null, null);

    flow1.initialise();
    flow2.initialise();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 2, 0, 0, 0, 0, 0);

    flow1.start();
    flow2.start();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 2, 0, 0, 2, 0, 0);

    flow1.stop();
    flow2.stop();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 2, 0, 0, 0, 0, 0);

    flow1.dispose();
    flow2.dispose();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 0, 0, 0, 0);
  }

  @Test
  public void triggerFlowRestarted() throws MuleException {
    DefaultFlowsSummaryStatistics flowsSummaryStatistics = new DefaultFlowsSummaryStatistics(true);
    TestPipeline flow = new TestPipeline("", muleContext,
                                         mock(MessageSource.class),
                                         emptyList(), empty(), empty(),
                                         INITIAL_STATE_STARTED,
                                         null,
                                         flowsSummaryStatistics,
                                         null, null);

    flow.initialise();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 0, 0, 0);

    flow.start();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 1, 0, 0);

    flow.stop();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 0, 0, 0);

    flow.start();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 1, 0, 0);

    flow.stop();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 0, 0, 0);

    flow.dispose();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 0, 0, 0, 0);
  }

  @Test
  public void triggerFlowInitialStateStopped() throws MuleException {
    doReturn(true).when(muleContext).isStarting();

    DefaultFlowsSummaryStatistics flowsSummaryStatistics = new DefaultFlowsSummaryStatistics(true);
    TestPipeline flow = new TestPipeline("", muleContext,
                                         mock(MessageSource.class),
                                         emptyList(), empty(), empty(),
                                         INITIAL_STATE_STOPPED,
                                         null,
                                         flowsSummaryStatistics,
                                         null, null);

    flow.initialise();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 0, 0, 0);

    flow.start();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 0, 0, 0);

    doReturn(false).when(muleContext).isStarting();
    flow.start();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 1, 0, 0);

    flow.stop();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 0, 0, 0);

    flow.dispose();
    assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 0, 0, 0, 0);
  }

  private void assertFlowsSummaryStatistics(FlowsSummaryStatistics flowsSummaryStatistics,
                                            int expectedDeclaredTriggerFlows,
                                            int expectedDeclaredApikitFlows,
                                            int expectedDeclaredPrivateFlows,
                                            int expectedActiveTriggerFlows,
                                            int expectedActiveApikitFlows,
                                            int expectedActivePrivateFlows) {
    assertThat("declaredTriggerFlows",
               flowsSummaryStatistics.getDeclaredTriggerFlows(), is(expectedDeclaredTriggerFlows));
    assertThat("declaredApikitFlows",
               flowsSummaryStatistics.getDeclaredApikitFlows(), is(expectedDeclaredApikitFlows));
    assertThat("declaredPrivateFlows",
               flowsSummaryStatistics.getDeclaredPrivateFlows(), is(expectedDeclaredPrivateFlows));
    assertThat("activeTriggerFlows",
               flowsSummaryStatistics.getActiveTriggerFlows(), is(expectedActiveTriggerFlows));
    assertThat("activeApikitFlows",
               flowsSummaryStatistics.getActiveApikitFlows(), is(expectedActiveApikitFlows));
    assertThat("activePrivateFlows",
               flowsSummaryStatistics.getActivePrivateFlows(), is(expectedActivePrivateFlows));
  }

  private static class TestPipeline extends AbstractPipeline {

    public TestPipeline(String name, MuleContext muleContext, MessageSource source, List<Processor> processors,
                        Optional<FlowExceptionHandler> exceptionListener,
                        Optional<ProcessingStrategyFactory> processingStrategyFactory, String initialState,
                        Integer maxConcurrency,
                        DefaultFlowsSummaryStatistics flowsSummaryStatistics, FlowConstructStatistics flowConstructStatistics,
                        ComponentInitialStateManager componentInitialStateManager) {
      super(name, muleContext, source, processors, exceptionListener, processingStrategyFactory, initialState, maxConcurrency,
            flowsSummaryStatistics, flowConstructStatistics, componentInitialStateManager);
    }

    @Override
    public ComponentLocation getLocation() {
      return mock(ComponentLocation.class, RETURNS_DEEP_STUBS);
    }

    @Override
    protected Function<? super CoreEvent, Mono<? extends CoreEvent>> flowFailDropMapper(Function<CoreEvent, CoreEvent> eventForFlowMapper,
                                                                                        BiFunction<CoreEvent, CoreEvent, CoreEvent> returnEventFromFlowMapper,
                                                                                        ErrorType overloadErrorType) {
      // nothing to do
      return null;
    }

    @Override
    protected Function<? super CoreEvent, Mono<? extends CoreEvent>> flowWaitMapper(Function<CoreEvent, CoreEvent> eventForFlowMapper,
                                                                                    BiFunction<CoreEvent, CoreEvent, CoreEvent> returnEventFromFlowMapper) {
      // nothing to do
      return null;
    }

    @Override
    public String getConstructType() {
      return "Flow";
    }
  }

}
