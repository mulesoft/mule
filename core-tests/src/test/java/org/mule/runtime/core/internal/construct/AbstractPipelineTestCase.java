/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.construct;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;
import org.junit.After;
import org.junit.Test;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.deployment.management.ComponentInitialStateManager;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.BackPressureReason;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.core.internal.construct.FlowBackPressureException.createFlowBackPressureException;

@SmallTest
public class AbstractPipelineTestCase extends AbstractMuleContextTestCase {

  private static class TestPipeline extends AbstractPipeline {

    public TestPipeline(String name, MuleContext muleContext, MessageSource source, List<Processor> processors,
                        Optional<FlowExceptionHandler> exceptionListener,
                        Optional<ProcessingStrategyFactory> processingStrategyFactory, String initialState,
                        Integer maxConcurrency, FlowConstructStatistics flowConstructStatistics,
                        ComponentInitialStateManager componentInitialStateManager) {
      super(name, muleContext, source, processors, exceptionListener, processingStrategyFactory, initialState, maxConcurrency,
            flowConstructStatistics, componentInitialStateManager);
    }

    @Override
    public ComponentLocation getLocation() {
      return mock(ComponentLocation.class, RETURNS_DEEP_STUBS);
    }
  }

  private AbstractPipeline abstractPipeline;

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();

    MessageSource mockMessageSource = mock(MessageSource.class);
    FlowConstructStatistics mockFlowConstructStatistics = mock(FlowConstructStatistics.class);
    ComponentInitialStateManager mockComponentInitialStateManager = mock(ComponentInitialStateManager.class);
    abstractPipeline = new TestPipeline(
                                        "TestPipeline",
                                        muleContext,
                                        mockMessageSource,
                                        emptyList(),
                                        empty(),
                                        empty(),
                                        "test initial state",
                                        1,
                                        mockFlowConstructStatistics,
                                        mockComponentInitialStateManager);
    abstractPipeline.initialise();
  }

  @After
  public void after() throws MuleException {
    if (abstractPipeline.getLifecycleState().isInitialised()) {
      abstractPipeline.dispose();
    }
  }

  @Test
  @Issue("MULE-20032")
  @Description("Checks that the pre-built back pressure exceptions have the same type and reason as defined by FlowBackPressureException")
  public void testBackpressureExceptionsHaveCorrectTypeAndReason() {
    for (BackPressureReason backPressureReason : BackPressureReason.values()) {
      // We expect that cached exceptions in the abstract pipeline would be created with the same internal mapping defined
      // by FlowBackPressureException
      FlowBackPressureException expectedFlowBackPressureException =
          createFlowBackPressureException(abstractPipeline, backPressureReason);
      FlowBackPressureException actualFlowBackPressureException =
          abstractPipeline.getBackPressureExceptions().get(backPressureReason);

      assertThat(actualFlowBackPressureException, instanceOf(expectedFlowBackPressureException.getClass()));
      assertThat(actualFlowBackPressureException.getMessage(), is(equalTo(expectedFlowBackPressureException.getMessage())));
    }
  }
}
