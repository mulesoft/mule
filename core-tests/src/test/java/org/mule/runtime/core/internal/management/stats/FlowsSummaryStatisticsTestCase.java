/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import static org.mule.runtime.core.api.construct.Flow.INITIAL_STATE_STARTED;
import static org.mule.runtime.core.api.construct.Flow.INITIAL_STATE_STOPPED;
import static org.mule.runtime.core.internal.management.stats.DefaultFlowsSummaryStatistics.isApiKitFlow;

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
import org.mule.runtime.core.api.MuleContext;
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

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Note: flow-mappings are not considered
 *
 */
@SmallTest
@Feature("")
@Story("")
public class FlowsSummaryStatisticsTestCase extends AbstractMuleContextTestCase {

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();

    muleContext = spy(muleContext);
  }

  ///////////////
  // Flow lifecycle tests
  ///////////////

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
    assertThat(flowsSummaryStatistics.getDeclaredTriggerFlows(), is(1));
    assertThat(flowsSummaryStatistics.getDeclaredPrivateFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActiveTriggerFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActivePrivateFlows(), is(0));

    flow.start();
    assertThat(flowsSummaryStatistics.getDeclaredTriggerFlows(), is(1));
    assertThat(flowsSummaryStatistics.getDeclaredPrivateFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActiveTriggerFlows(), is(1));
    assertThat(flowsSummaryStatistics.getActivePrivateFlows(), is(0));

    flow.stop();
    assertThat(flowsSummaryStatistics.getDeclaredTriggerFlows(), is(1));
    assertThat(flowsSummaryStatistics.getDeclaredPrivateFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActiveTriggerFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActivePrivateFlows(), is(0));

    flow.dispose();
    assertThat(flowsSummaryStatistics.getDeclaredTriggerFlows(), is(0));
    assertThat(flowsSummaryStatistics.getDeclaredPrivateFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActiveTriggerFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActivePrivateFlows(), is(0));
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
    assertThat(flowsSummaryStatistics.getDeclaredTriggerFlows(), is(0));
    assertThat(flowsSummaryStatistics.getDeclaredPrivateFlows(), is(1));
    assertThat(flowsSummaryStatistics.getActiveTriggerFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActivePrivateFlows(), is(0));

    flow.start();
    assertThat(flowsSummaryStatistics.getDeclaredTriggerFlows(), is(0));
    assertThat(flowsSummaryStatistics.getDeclaredPrivateFlows(), is(1));
    assertThat(flowsSummaryStatistics.getActiveTriggerFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActivePrivateFlows(), is(1));

    flow.stop();
    assertThat(flowsSummaryStatistics.getDeclaredTriggerFlows(), is(0));
    assertThat(flowsSummaryStatistics.getDeclaredPrivateFlows(), is(1));
    assertThat(flowsSummaryStatistics.getActiveTriggerFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActivePrivateFlows(), is(0));

    flow.dispose();
    assertThat(flowsSummaryStatistics.getDeclaredTriggerFlows(), is(0));
    assertThat(flowsSummaryStatistics.getDeclaredPrivateFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActiveTriggerFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActivePrivateFlows(), is(0));
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
    assertThat(flowsSummaryStatistics.getDeclaredTriggerFlows(), is(2));
    assertThat(flowsSummaryStatistics.getDeclaredPrivateFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActiveTriggerFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActivePrivateFlows(), is(0));

    flow1.start();
    flow2.start();
    assertThat(flowsSummaryStatistics.getDeclaredTriggerFlows(), is(2));
    assertThat(flowsSummaryStatistics.getDeclaredPrivateFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActiveTriggerFlows(), is(2));
    assertThat(flowsSummaryStatistics.getActivePrivateFlows(), is(0));

    flow1.stop();
    flow2.stop();
    assertThat(flowsSummaryStatistics.getDeclaredTriggerFlows(), is(2));
    assertThat(flowsSummaryStatistics.getDeclaredPrivateFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActiveTriggerFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActivePrivateFlows(), is(0));

    flow1.dispose();
    flow2.dispose();
    assertThat(flowsSummaryStatistics.getDeclaredTriggerFlows(), is(0));
    assertThat(flowsSummaryStatistics.getDeclaredPrivateFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActiveTriggerFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActivePrivateFlows(), is(0));
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
    assertThat(flowsSummaryStatistics.getDeclaredTriggerFlows(), is(1));
    assertThat(flowsSummaryStatistics.getDeclaredPrivateFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActiveTriggerFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActivePrivateFlows(), is(0));

    flow.start();
    assertThat(flowsSummaryStatistics.getDeclaredTriggerFlows(), is(1));
    assertThat(flowsSummaryStatistics.getDeclaredPrivateFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActiveTriggerFlows(), is(1));
    assertThat(flowsSummaryStatistics.getActivePrivateFlows(), is(0));

    flow.stop();
    assertThat(flowsSummaryStatistics.getDeclaredTriggerFlows(), is(1));
    assertThat(flowsSummaryStatistics.getDeclaredPrivateFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActiveTriggerFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActivePrivateFlows(), is(0));

    flow.start();
    assertThat(flowsSummaryStatistics.getDeclaredTriggerFlows(), is(1));
    assertThat(flowsSummaryStatistics.getDeclaredPrivateFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActiveTriggerFlows(), is(1));
    assertThat(flowsSummaryStatistics.getActivePrivateFlows(), is(0));

    flow.stop();
    assertThat(flowsSummaryStatistics.getDeclaredTriggerFlows(), is(1));
    assertThat(flowsSummaryStatistics.getDeclaredPrivateFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActiveTriggerFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActivePrivateFlows(), is(0));

    flow.dispose();
    assertThat(flowsSummaryStatistics.getDeclaredTriggerFlows(), is(0));
    assertThat(flowsSummaryStatistics.getDeclaredPrivateFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActiveTriggerFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActivePrivateFlows(), is(0));
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
    assertThat(flowsSummaryStatistics.getDeclaredTriggerFlows(), is(1));
    assertThat(flowsSummaryStatistics.getDeclaredPrivateFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActiveTriggerFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActivePrivateFlows(), is(0));

    flow.start();
    assertThat(flowsSummaryStatistics.getDeclaredTriggerFlows(), is(1));
    assertThat(flowsSummaryStatistics.getDeclaredPrivateFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActiveTriggerFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActivePrivateFlows(), is(0));

    doReturn(false).when(muleContext).isStarting();
    flow.start();
    assertThat(flowsSummaryStatistics.getDeclaredTriggerFlows(), is(1));
    assertThat(flowsSummaryStatistics.getDeclaredPrivateFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActiveTriggerFlows(), is(1));
    assertThat(flowsSummaryStatistics.getActivePrivateFlows(), is(0));

    flow.stop();
    assertThat(flowsSummaryStatistics.getDeclaredTriggerFlows(), is(1));
    assertThat(flowsSummaryStatistics.getDeclaredPrivateFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActiveTriggerFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActivePrivateFlows(), is(0));

    flow.dispose();
    assertThat(flowsSummaryStatistics.getDeclaredTriggerFlows(), is(0));
    assertThat(flowsSummaryStatistics.getDeclaredPrivateFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActiveTriggerFlows(), is(0));
    assertThat(flowsSummaryStatistics.getActivePrivateFlows(), is(0));
  }

  private static class TestPipeline extends AbstractPipeline {

    public TestPipeline(String name, MuleContext muleContext, MessageSource source, List<Processor> processors,
                        Optional<FlowExceptionHandler> exceptionListener,
                        Optional<ProcessingStrategyFactory> processingStrategyFactory, String initialState,
                        Integer maxConcurrency,
                        FlowsSummaryStatistics flowsSummaryStatistics, FlowConstructStatistics flowConstructStatistics,
                        ComponentInitialStateManager componentInitialStateManager) {
      super(name, muleContext, source, processors, exceptionListener, processingStrategyFactory, initialState, maxConcurrency,
            flowsSummaryStatistics, flowConstructStatistics, componentInitialStateManager);
    }

    @Override
    public ComponentLocation getLocation() {
      return mock(ComponentLocation.class, RETURNS_DEEP_STUBS);
    }
  }

  ///////////////
  // ApiKit naming tests
  ///////////////

  @Test
  public void noApikitFlowSimpleName() {
    assertThat(isApiKitFlow("privateFlow"), is(false));
  }

  @Test
  // TODO can this also have a content type?
  public void apikitSoapFlowMethod() {
    assertThat(isApiKitFlow("ListInventory:\\config"), is(true));
  }

  @Test
  public void apikitFlowMethodGet() {
    assertThat(isApiKitFlow("get:\\reservation:api-config"), is(true));
  }

  @Test
  public void apikitFlowMethodPost() {
    assertThat(isApiKitFlow("post:\\reservation:api-config"), is(true));
  }

  /**
   * The HTTP methods may be anything, not just the commonly used ones. So, since APIKIT may route a request with any method, the
   * flows handling these methods are valid and must be considered as APIKIT flows.
   */
  @Test
  public void apikitFlowMethodAnything() {
    assertThat(isApiKitFlow("randomize:\\reservation:api-config"), is(true));

  }

  @Test
  public void apikitFlowWithContentType() {
    assertThat(isApiKitFlow("put:\\accounts:application\\json:account-domain-api-config"), is(true));
  }

  @Test
  public void apikitFlowMethodPathWithMorePathParts() {
    assertThat(isApiKitFlow("delete:\\accounts\\myAccount:account-domain-api-config"), is(true));
  }

  @Test
  public void apikitFlowMethodPathWithParamPathPart() {
    assertThat(isApiKitFlow("delete:\\accounts\\(id):account-domain-api-config"), is(true));
  }

  @Test
  public void apikitFlowMethodPathWithPathPartWithNumbers() {
    assertThat(isApiKitFlow("delete:\\accounts101\\(id):account-domain-api-config"), is(true));
  }

  @Test
  public void apikitFlowMethodPathWithPathPartWithSpecialChars() {
    assertThat(isApiKitFlow("delete:\\accounts.-_~!$&'()*+,;=:@\\(id):account-domain-api-config"), is(true));
  }

  @Test
  public void apikitFlowMethodPathWithPathPartWithEncodedChars() {
    assertThat(isApiKitFlow("delete:\\accounts\\(id)\\%00:account-domain-api-config"), is(true));
  }

}
