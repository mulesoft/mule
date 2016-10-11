/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.chain;

import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.Event.setCurrentEvent;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;

import org.mule.runtime.core.TransformationService;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ProcessorExecutor;
import org.mule.runtime.core.context.notification.DefaultFlowCallStack;
import org.mule.runtime.core.execution.MessageProcessorExecutionTemplate;
import org.mule.runtime.core.processor.BlockingProcessorExecutor;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class BlockingProcessorExecutorTestCase extends AbstractMuleContextTestCase {

  @Mock
  protected Event event;

  @Mock
  protected MessageProcessorExecutionTemplate executionTemplate;

  protected static String A = "a";
  protected static String B = "b";
  protected static String C = "c";
  protected static String RESULT = A + B + C;

  protected SensingNullMessageProcessor processor1 = new SensingNullMessageProcessor(A);
  protected SensingNullMessageProcessor processor2 = new SensingNullMessageProcessor(B);
  protected SensingNullMessageProcessor processor3 = new SensingNullMessageProcessor(C);
  protected List<Processor> processors = new ArrayList<>();

  @Before
  public void before() throws Exception {
    processors.add(processor1);
    processors.add(processor2);
    processors.add(processor3);

    setCurrentEvent(event);

    InternalMessage message = InternalMessage.builder().payload("").build();
    when(event.getMessage()).thenReturn(message);
    when(event.getFlowCallStack()).thenReturn(new DefaultFlowCallStack());
    when(event.getError()).thenReturn(empty());
    when(executionTemplate.execute(any(Processor.class), any(Event.class))).thenAnswer(invocation -> {
      Processor messageProcessor = (Processor) invocation.getArguments()[0];
      Event event = (Event) invocation.getArguments()[1];
      return messageProcessor.process(event);
    });
    muleContext.setTransformationService(new TransformationService(muleContext));
  }

  @After
  public void after() {
    processor1.dispose();
    processor2.dispose();
    processor3.dispose();
  }

  @Test
  public void executeRequestResponse() throws MuleException {
    setupRequestResponseEvent();
    assertBlockingExecution(processors);
  }

  @Test
  public void executeRequestResponseNullResponse() throws MuleException {
    processors.add(event -> null);
    setupRequestResponseEvent();
    assertThat(createProcessorExecutor(processors).execute(), is(nullValue()));
  }

  @Test
  public void executeOneWay() throws MuleException, InterruptedException {
    setupOneWayEvent();
    assertBlockingExecution(processors);
  }

  protected void setupOneWayEvent() {
    when(event.getExchangePattern()).thenReturn(ONE_WAY);
    when(event.isSynchronous()).thenReturn(false);
  }

  protected void setupRequestResponseEvent() {
    when(event.getExchangePattern()).thenReturn(REQUEST_RESPONSE);
    when(event.isSynchronous()).thenReturn(true);
  }

  protected void assertBlockingExecution(List<Processor> processors)
      throws MuleException {
    ProcessorExecutor executor = createProcessorExecutor(processors);

    if (event.getExchangePattern() == REQUEST_RESPONSE) {
      assertThat(executor.execute().getMessageAsString(muleContext), equalTo(RESULT));
    } else {
      assertThat(executor.execute().getMessage(), equalTo(event.getMessage()));
    }

    assertThat(processor1.event, is(notNullValue()));
    assertThat(processor1.thread, equalTo(Thread.currentThread()));

    assertThat(processor2.event, is(notNullValue()));
    assertThat(processor2.thread, equalTo(Thread.currentThread()));

    assertThat(processor3.event, is(notNullValue()));
    assertThat(processor3.thread, equalTo(Thread.currentThread()));
  }

  protected ProcessorExecutor createProcessorExecutor(List<Processor> processors) {
    return new BlockingProcessorExecutor(event, processors, executionTemplate, true);
  }
}
