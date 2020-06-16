/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mule.tck.MuleTestUtils.APPLE_FLOW;
import static org.mule.tck.MuleTestUtils.createAndRegisterFlow;

import io.qameta.allure.Issue;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerConfig;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.construct.FromFlowRejectedExecutionException;
import org.mule.runtime.core.internal.processor.strategy.StreamPerEventSink;
import org.mule.tck.SimpleUnitTestSupportSchedulerService;

import java.util.concurrent.RejectedExecutionException;

@Issue("MULE-18431")
public class AsyncDelegateMessageProcessorBackPressureTestCase extends AbstractAsyncDelegateMessageProcessorTestCase {

  public AsyncDelegateMessageProcessorBackPressureTestCase(Mode mode) {
    super(mode);
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    BackPressureGeneratorProcessingStrategy strategy = new BackPressureGeneratorProcessingStrategy();
    flow = createAndRegisterFlow(muleContext, APPLE_FLOW, componentLocator, (ctx, n) -> strategy);
    async = createAsyncDelegateMessageProcessor(target, flow);
    async.setSchedulerService(new FixingBackPressureSchedulerService(strategy));
    async.start();
  }

  private class BackPressureGeneratorProcessingStrategy implements ProcessingStrategy {

    private boolean backPressure = true;

    @Override
    public boolean isSynchronous() {
      return true;
    }

    @Override
    public Sink createSink(FlowConstruct flowConstruct, ReactiveProcessor pipeline) {
      return new StreamPerEventSink(pipeline, event -> {
      });
    }

    @Override
    public void checkBackpressureAccepting(CoreEvent event) throws RejectedExecutionException {
      if (backPressure) {
        throw new FromFlowRejectedExecutionException(null);
      }
    }

    public void setBackPressure(boolean backPressure) {
      this.backPressure = backPressure;
    }

  }

  private class FixingBackPressureSchedulerService extends SimpleUnitTestSupportSchedulerService {

    private BackPressureGeneratorProcessingStrategy strategy;

    public FixingBackPressureSchedulerService(BackPressureGeneratorProcessingStrategy strategy) {
      this.strategy = strategy;
    }

    @Override
    public Scheduler customScheduler(SchedulerConfig config) {
      Scheduler scheduler = mock(Scheduler.class);
      doAnswer(invocationOnMock -> {
        strategy.setBackPressure(false);
        ((Runnable) invocationOnMock.getArgument(0)).run();
        return null;
      }).when(scheduler).execute(any());
      return scheduler;
    }
  }
}
