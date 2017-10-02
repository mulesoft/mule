/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;

import static java.lang.System.lineSeparator;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.tck.junit4.AbstractMuleContextTestCase.RECEIVE_TIMEOUT;
import static org.mule.tck.processor.FlowAssert.addAssertion;
import static reactor.core.Exceptions.propagate;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.el.ValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.expression.InvalidExpressionException;
import org.mule.runtime.core.api.processor.InterceptingMessageProcessor;
import org.mule.runtime.core.api.processor.Processor;

import org.reactivestreams.Publisher;

import com.eaio.uuid.UUID;

import java.util.concurrent.CountDownLatch;

import reactor.core.publisher.Flux;

public class ResponseAssertionMessageProcessor extends AssertionMessageProcessor
    implements InterceptingMessageProcessor, Startable {

  private static final ThreadLocal<String> taskTokenInThread = new ThreadLocal<>();

  protected String responseExpression = "#[true]";
  private int responseCount = 1;
  private boolean responseSameTask = true;

  private Processor next;
  private String requestTaskToken;
  private String responseTaskToken;
  private String responseStackTrace;
  private CountDownLatch responseLatch;
  private int responseInvocationCount = 0;
  private boolean responseResult = true;

  @Override
  public void start() throws InitialisationException {
    super.start();
    ValidationResult result = this.expressionManager.validate(responseExpression);
    if (!result.isSuccess()) {
      throw new InvalidExpressionException(expression, result.errorMessage().orElse("Invalid expression"));
    }
    responseLatch = new CountDownLatch(responseCount);
    addAssertion(getLocation().getRootContainerName(), this);
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    if (event == null) {
      return null;
    }
    return processResponse(processNext(processRequest(event)));
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    Flux<CoreEvent> flux = from(publisher).map(event -> {
      try {
        return processRequest(event);
      } catch (MuleException e) {
        throw propagate(e);
      }
    });
    flux = from(flux.transform(next));
    return flux.map(event -> {
      try {
        return processResponse(event);
      } catch (MuleException e) {
        throw propagate(e);
      }
    });
  }

  private CoreEvent processRequest(CoreEvent event) throws MuleException {
    if (taskTokenInThread.get() != null) {
      requestTaskToken = taskTokenInThread.get();
    } else {
      requestTaskToken = generateTaskToken();
      taskTokenInThread.set(requestTaskToken);
    }
    return super.process(event);
  }

  private CoreEvent processResponse(CoreEvent event) throws MuleException {
    if (event == null) {
      return event;
    }

    if (taskTokenInThread.get() != null) {
      responseTaskToken = taskTokenInThread.get();
    } else {
      responseTaskToken = generateTaskToken();
    }
    responseStackTrace = getStackTrace(new Exception());

    responseResult = responseResult && expressionManager.evaluateBoolean(responseExpression, event, getLocation(), false, true);
    increaseResponseCount();
    responseLatch.countDown();
    return event;
  }

  protected String generateTaskToken() {
    return currentThread().getName() + " - " + new UUID().toString();
  }

  private CoreEvent processNext(CoreEvent event) throws MuleException {
    if (event != null) {
      return next.process(event);
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
    } else if (responseCount > 0 && responseSameTask) {
      assertThat(failureMessagePrefix() + "Response task was not same as request task", responseTaskToken,
                 is(requestTaskToken));
    } else if (responseCount > 0 && !responseSameTask) {
      assertThat(failureMessagePrefix() + "Response task was same as request task. Response stack trace is " + lineSeparator()
          + responseStackTrace, responseTaskToken, not(is(requestTaskToken)));
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

  public void setResponseSameTask(boolean responseSameTask) {
    this.responseSameTask = responseSameTask;
  }

  /**
   * The semantics of the count are as follows: - count was set & count processes were done => ok - count was set & count
   * processes were not done => fail - count was not set & at least one processing were done => ok
   *
   * @return
   * @throws InterruptedException
   */
  synchronized private boolean isResponseProcessesCountCorrect() throws InterruptedException {
    boolean countReached = responseLatch.await(RECEIVE_TIMEOUT, MILLISECONDS);
    if (needToMatchCount) {
      return responseCount == responseInvocationCount;
    } else {
      return countReached;
    }
  }

}
