/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.chain;

import static java.lang.Thread.currentThread;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.NonBlockingVoidMuleEvent;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorContainer;
import org.mule.runtime.core.api.processor.MessageProcessorPathElement;
import org.mule.runtime.core.api.processor.ProcessorExecutor;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.processor.AbstractInterceptingMessageProcessor;
import org.mule.runtime.core.processor.NonBlockingProcessorExecutor;
import org.mule.runtime.core.processor.strategy.NonBlockingProcessingStrategy;
import org.mule.tck.SensingNullReplyToHandler;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NonBlockingProcessorExecutorTestCase extends BlockingProcessorExecutorTestCase {

  private static final int LATCH_TIMEOUT = 50;
  private static final String TEST_MESSAGE = "abc";

  private SensingNullReplyToHandler nullReplyToHandler = new SensingNullReplyToHandler();

  @Override
  public void before() throws Exception {
    super.before();
    when(event.getExchangePattern()).thenReturn(MessageExchangePattern.REQUEST_RESPONSE);
    when(event.isSynchronous()).thenReturn(false);
    when(event.isAllowNonBlocking()).thenReturn(true);
    Pipeline mockFlow = mock(Flow.class);
    when(mockFlow.getMuleContext()).thenReturn(muleContext);
    when(mockFlow.getProcessingStrategy()).thenReturn(new NonBlockingProcessingStrategy());
    when(event.getFlowConstruct()).thenReturn(mockFlow);
  }

  @Test
  public void executeRequestResponseNonBlocking() throws MuleException, InterruptedException {
    setupNonBlockingRequestResponseEvent();
    assertNonBlockingExecutionWithReplyTo(processors);
  }

  @Test
  public void executeRequestResponseWithInterceptingMPBlocking() throws MuleException, InterruptedException {
    processors.clear();
    processors.add(new AbstractInterceptingMessageProcessor() {

      @Override
      public MuleEvent process(MuleEvent event) throws MuleException {
        return processNext(event);
      }
    });
    processors.add(processor1);
    processors.add(processor2);
    processors.add(processor3);
    assertBlockingExecution(processors, not(sameInstance(event)));

    // When NonBlockingProcessorExecutor falls-back to blocking the existing ReplyToHandler should be cleared
    assertThat(processor1.event.getReplyToHandler(), is(nullValue()));
  }

  @Test
  public void executeRequestResponseWithMPContainerBlocking() throws MuleException, InterruptedException {
    processors.clear();
    processors.add(new TestContainerMessageProcessor());
    processors.add(processor2);
    processors.add(processor3);
    assertBlockingExecution(processors, not(sameInstance(event)));

    // When NonBlockingProcessorExecutor falls-back to blocking the existing ReplyToHandler should be cleared
    assertThat(processor1.event.getReplyToHandler(), is(nullValue()));
  }

  @Test
  public void executeRequestResponseNonBlockingNullResponse() throws MuleException, InterruptedException {
    processors.add(event -> null);
    setupNonBlockingRequestResponseEvent();
    createProcessorExecutor(processors).execute();
    assertThat(nullReplyToHandler.latch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS), is(true));
    assertThat(nullReplyToHandler.event, is(nullValue()));
  }

  @Test
  public void executeRequestResponseNonBlockingVoidResponse() throws MuleException, InterruptedException {
    final MuleEvent voidResult = VoidMuleEvent.getInstance();
    processors.add(event -> voidResult);
    setupNonBlockingRequestResponseEvent();
    MuleEvent request = event;
    createProcessorExecutor(processors).execute();
    assertThat(nullReplyToHandler.latch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS), is(true));
    assertThat(nullReplyToHandler.event, is(not(nullValue())));
    assertThat(nullReplyToHandler.event, CoreMatchers.<MuleEvent>not(VoidMuleEvent.getInstance()));
  }

  @Override
  @Test
  public void executeOneWay() throws MuleException, InterruptedException {
    setupOneWayEvent();
    assertNonBlockingExecutionWithoutReplyTo(processors);
  }

  @Test
  public void executeOneWayWithReplyTo() throws MuleException, InterruptedException {
    setupOneWayEvent();
    when(event.getReplyToHandler()).thenReturn(nullReplyToHandler);
    assertNonBlockingExecutionWithReplyTo(processors);
  }

  private class TestContainerMessageProcessor implements MessageProcessor, MessageProcessorContainer {

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException {
      return processor1.process(event);
    }

    @Override
    public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement) {

    }
  }


  @Override
  protected ProcessorExecutor createProcessorExecutor(List<MessageProcessor> processors) {
    return new NonBlockingProcessorExecutor(event, processors, executionTemplate, true);
  }

  private void assertNonBlockingExecution(List<MessageProcessor> processors) throws MuleException, InterruptedException {
    ProcessorExecutor executor = createProcessorExecutor(processors);

    if (event.isAllowNonBlocking()) {
      assertThat(executor.execute(), equalTo(NonBlockingVoidMuleEvent.getInstance()));
    } else {
      assertThat(executor.execute(), equalTo(event));
    }

    assertThat(processor3.latch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS), is(true));

    assertThat(processor1.event, is(notNullValue()));
    assertThat(processor1.thread, not(equalTo(currentThread())));

    assertThat(processor2.event, is(notNullValue()));
    assertThat(processor2.thread, not(equalTo(processor1.thread)));

    assertThat(processor3.event, is(notNullValue()));
    assertThat(processor3.thread, not(equalTo(processor2.thread)));
  }

  private void assertNonBlockingExecutionWithReplyTo(List<MessageProcessor> processors)
      throws MuleException, InterruptedException {
    assertNonBlockingExecution(processors);
    assertThat(nullReplyToHandler.latch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS), is(true));
    assertThat(nullReplyToHandler.event.getMessageAsString(), equalTo(RESULT));
  }

  private void assertNonBlockingExecutionWithoutReplyTo(List<MessageProcessor> processors)
      throws MuleException, InterruptedException {
    assertNonBlockingExecution(processors);
    assertThat(nullReplyToHandler.latch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS), is(false));
  }

  private void setupNonBlockingRequestResponseEvent() {
    when(event.getExchangePattern()).thenReturn(MessageExchangePattern.REQUEST_RESPONSE);
    when(event.isSynchronous()).thenReturn(false);
    when(event.isAllowNonBlocking()).thenReturn(true);
    when(event.getReplyToHandler()).thenReturn(nullReplyToHandler);
  }

}
