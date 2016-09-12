/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.functional;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.runtime.core.execution.MessageProcessorExecutionTemplate.createExceptionTransformerExecutionTemplate;

import org.mule.runtime.core.NonBlockingVoidMuleEvent;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.InternalMessage;
import org.mule.runtime.core.api.NonBlockingSupported;
import org.mule.runtime.core.api.connector.NonBlockingReplyToHandler;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.processor.InterceptingMessageProcessor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.processor.chain.ProcessorExecutorFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ResponseAssertionMessageProcessor extends AssertionMessageProcessor
    implements InterceptingMessageProcessor, FlowConstructAware, Startable, NonBlockingSupported {

  protected String responseExpression = "#[true]";
  private int responseCount = 1;
  private boolean responseSameThread = true;

  private Processor next;
  private Thread requestThread;
  private Thread responseThread;
  private CountDownLatch responseLatch;
  private int responseInvocationCount = 0;
  private Event responseEvent;
  private boolean responseResult = true;

  @Override
  public void start() throws InitialisationException {
    super.start();
    this.expressionLanguage.validate(responseExpression);
    responseLatch = new CountDownLatch(responseCount);
    FlowAssert.addAssertion(flowConstruct.getName(), this);
  }

  @Override
  public Event process(Event event) throws MuleException {
    if (event == null) {
      return null;
    }

    if (event.isAllowNonBlocking() && event.getReplyToHandler() != null) {
      final ReplyToHandler originalReplyToHandler = event.getReplyToHandler();
      event = Event.builder(event).replyToHandler(new NonBlockingReplyToHandler() {

        @Override
        public Event processReplyTo(Event event, InternalMessage returnMessage, Object replyTo) throws MuleException {
          return originalReplyToHandler.processReplyTo(processResponse(event), null, null);
        }

        @Override
        public void processExceptionReplyTo(MessagingException exception, Object replyTo) {
          originalReplyToHandler.processExceptionReplyTo(exception, replyTo);
        }
      }).build();
    }
    Event result = processNext(processRequest(event));
    if (!(result instanceof NonBlockingVoidMuleEvent)) {
      return processResponse(result);
    } else {
      return result;
    }
  }

  public Event processRequest(Event event) throws MuleException {
    requestThread = Thread.currentThread();
    return super.process(event);
  }

  public Event processResponse(Event event) throws MuleException {
    if (event == null || VoidMuleEvent.getInstance().equals(event)) {
      return event;
    }
    responseThread = Thread.currentThread();
    this.responseEvent = event;
    responseResult = responseResult && expressionLanguage.evaluateBoolean(responseExpression, event, flowConstruct, false, true);
    increaseResponseCount();
    responseLatch.countDown();
    return event;
  }

  private Event processNext(Event event) throws MuleException {
    if (event != null || event instanceof VoidMuleEvent) {
      return new ProcessorExecutorFactory()
          .createProcessorExecutor(event, singletonList(next), createExceptionTransformerExecutionTemplate(), false,
                                   flowConstruct)
          .execute();
    } else {
      return event;
    }
  }

  @Override
  public void verify() throws InterruptedException {
    super.verify();
    if (responseCountFailOrNullEvent()) {
      fail(failureMessagePrefix() + "No response message received or if responseCount "
          + "attribute was set then it was no matched.");
    } else if (responseExpressionFailed()) {
      fail(failureMessagePrefix() + "Response expression " + expression + " evaluated false.");
    } else if (responseCount > 0 && responseSameThread && (requestThread != responseThread)) {
      assertThat(failureMessagePrefix() + "Response thread was not same as request thread", responseThread,
                 sameInstance(requestThread));
    } else if (responseCount > 0 && !responseSameThread) {
      assertThat(failureMessagePrefix() + "Response thread was same as request thread", responseThread,
                 not(sameInstance(requestThread)));
    }
  }

  public Boolean responseCountFailOrNullEvent() throws InterruptedException {
    return !isResponseProcessesCountCorrect();
  }

  // added for testing (can't assert on asserts)
  public Boolean responseExpressionFailed() {
    return !responseResult;
  }

  @Override
  public void setListener(Processor listener) {
    this.next = listener;
  }

  private void increaseResponseCount() {
    responseInvocationCount++;
  }

  public void setResponseExpression(String responseExpression) {
    this.responseExpression = responseExpression;
  }

  public void setResponseCount(int responseCount) {
    this.responseCount = responseCount;
  }

  public void setResponseSameThread(boolean responseSameThread) {
    this.responseSameThread = responseSameThread;
  }

  /**
   * The semantics of the count are as follows: - count was set & count processes were done => ok - count was set & count
   * processes were not done => fail - count was not set & at least one processing were done => ok
   *
   * @return
   * @throws InterruptedException
   */
  synchronized private boolean isResponseProcessesCountCorrect() throws InterruptedException {
    boolean countReached = responseLatch.await(timeout, TimeUnit.MILLISECONDS);
    if (needToMatchCount) {
      return responseCount == responseInvocationCount;
    } else {
      return countReached;
    }
  }
}
