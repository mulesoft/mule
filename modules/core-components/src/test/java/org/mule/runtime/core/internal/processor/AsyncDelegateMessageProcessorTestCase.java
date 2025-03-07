/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor;

import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.core.api.construct.Flow.builder;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChain;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.from;
import static org.mule.tck.MuleTestUtils.APPLE_FLOW;
import static org.mule.tck.MuleTestUtils.createAndRegisterFlow;
import static org.mule.tck.processor.ContextPropagationChecker.assertContextPropagation;
import static org.mule.tck.util.MuleContextUtils.getNotificationDispatcher;
import static org.mule.test.allure.AllureConstants.ScopeFeature.SCOPE;
import static org.mule.test.allure.AllureConstants.ScopeFeature.AsyncStory.ASYNC;

import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.internal.processor.strategy.BlockingProcessingStrategyFactory;
import org.mule.runtime.core.internal.processor.strategy.DirectProcessingStrategyFactory;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.tck.processor.ContextPropagationChecker;
import org.mule.tck.testmodels.mule.TestTransactionFactory;

import org.junit.Ignore;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(SCOPE)
@Story(ASYNC)
public class AsyncDelegateMessageProcessorTestCase extends AbstractAsyncDelegateMessageProcessorTestCase {

  public AsyncDelegateMessageProcessorTestCase(Mode mode) {
    super(mode);
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    flow = createAndRegisterFlow(muleContext, APPLE_FLOW, componentLocator);
    async = createAsyncDelegateMessageProcessor(target, flow);
    async.start();
  }

  @Test
  public void processWithTx() throws Exception {
    Transaction transaction = new TestTransactionFactory(false)
        .beginTransaction("appName", getNotificationDispatcher(muleContext));

    try {
      CoreEvent request = testEvent();
      CoreEvent result = process(async, request);

      // Wait until processor in async is executed to allow assertions on sensed event
      asyncEntryLatch.countDown();
      assertThat(latch.await(LOCK_TIMEOUT, MILLISECONDS), is(true));

      assertTargetEvent(request);
      assertResponse(result);
    } finally {
      transaction.commit();
    }
  }

  @Test
  @Ignore("Does this case actually make sense? Async has to run in an independent context, so propagation from the parent is not a wanted thing.")
  public void subscriberContextPropagation() throws Exception {
    final ContextPropagationChecker contextPropagationChecker = new ContextPropagationChecker();

    async = createAsyncDelegateMessageProcessor(newChain(empty(), contextPropagationChecker, target), flow);
    async.setAnnotations(getAppleFlowComponentLocationAnnotations());
    initialiseIfNeeded(async, true, muleContext);
    async.start();

    assertContextPropagation(testEvent(), async, contextPropagationChecker);
    asyncEntryLatch.countDown();
    assertThat(latch.await(LOCK_TIMEOUT, MILLISECONDS), is(true));
  }

  @Test
  public void processWithBlockingProcessingStrategy() throws Exception {
    flow.dispose();
    flow = builder("flow", muleContext).processingStrategyFactory(new BlockingProcessingStrategyFactory()).build();
    flow.setAnnotations(singletonMap(LOCATION_KEY, from("flow")));
    flow.initialise();
    flow.start();

    process();
  }

  @Test
  public void processWithDirectProcessingStrategy() throws Exception {
    flow.dispose();
    flow = builder("flow", muleContext).processingStrategyFactory(new DirectProcessingStrategyFactory()).build();
    flow.setAnnotations(singletonMap(LOCATION_KEY, from("flow")));
    flow.initialise();
    flow.start();

    process();
  }

  @Test
  public void process() throws Exception {
    CoreEvent request = testEvent();

    CoreEvent result = process(async, request);

    // Complete parent context so we can assert event context completion based on async completion.
    ((BaseEventContext) request.getContext()).success(result);

    assertThat(((BaseEventContext) request.getContext()).isTerminated(), is(false));

    // Permit async processing now we have already asserted that response alone is not enough to complete event context.
    asyncEntryLatch.countDown();

    assertThat(latch.await(LOCK_TIMEOUT, MILLISECONDS), is(true));

    // Block until async completes, not just target processor.
    while (!((BaseEventContext) target.sensedEvent.getContext()).isTerminated()) {
      park100ns();
    }
    assertThat(target.sensedEvent, notNullValue());

    // Block to ensure async fully completes before testing state
    while (!((BaseEventContext) request.getContext()).isTerminated()) {
      park100ns();
    }

    assertThat(((BaseEventContext) target.sensedEvent.getContext()).isTerminated(), is(true));
    assertThat(((BaseEventContext) request.getContext()).isTerminated(), is(true));

    assertTargetEvent(request);
    assertResponse(result);
  }

}
