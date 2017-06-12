/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;

import static org.junit.Assert.fail;
import org.mule.runtime.api.el.ValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.expression.InvalidExpressionException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AssertionMessageProcessor implements Processor, FlowConstructAware, Startable {

  protected String expression = "#[mel:true]";
  protected String message = "?";
  private int count = 1;
  private int invocationCount = 0;
  protected boolean needToMatchCount = false;

  public void setExpression(String expression) {
    this.expression = expression;
  }

  protected int timeout = AbstractMuleContextTestCase.RECEIVE_TIMEOUT;

  private Event event;
  private CountDownLatch latch;

  protected FlowConstruct flowConstruct;
  protected ExpressionManager expressionManager;
  private boolean result = true;

  @Override
  public void start() throws InitialisationException {
    this.expressionManager = flowConstruct.getMuleContext().getExpressionManager();
    ValidationResult result = this.expressionManager.validate(expression);
    if (!result.isSuccess()) {
      throw new InvalidExpressionException(expression, result.errorMessage().orElse("Invalid exception"));
    }
    latch = new CountDownLatch(count);
    FlowAssert.addAssertion(flowConstruct.getName(), this);
  }

  @Override
  public Event process(Event event) throws MuleException {
    if (event == null) {
      return null;
    }
    this.event = event;
    result = result && expressionManager.evaluateBoolean(expression, event, flowConstruct, false, true);
    increaseCount();
    latch.countDown();
    return event;
  }

  /**
   * If result evaluated to false in some processed event or the last processed event was null, then assert fails, otherwise:
   * <li>count was set & count processes were done => ok</li>
   * <li>count was set & count processes were not done => fail</li>
   * <li>count was not set & at least one processing were done => ok</li>
   * 
   * @throws InterruptedException
   */
  public void verify() throws InterruptedException {
    if (countFailOrNullEvent()) {
      fail(failureMessagePrefix() + "No message received or if count attribute was " + "set then it was no matched.");
    } else if (expressionFailed()) {
      fail(failureMessagePrefix() + "Expression " + expression + " evaluated false.");
    }
  }

  protected String failureMessagePrefix() {
    String processorPath = "?";
    return "Flow assertion '" + message + "' failed @ '" + processorPath + "'. ";
  }


  public Boolean countFailOrNullEvent() throws InterruptedException // added for testing (cant assert on asserts)
  {
    return !isProcessesCountCorrect();
  }

  public Boolean expressionFailed() // added for testing (cant assert on asserts)
  {
    return !result;
  }

  public void reset() {
    this.event = null;
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public void setCount(int count) {
    this.count = count;
    needToMatchCount = true;
  }

  private void increaseCount() {
    invocationCount++;
  }

  /**
   * The semantics of the count are as follows: - count was set & count processes were done => ok - count was set & count
   * processes were not done => fail - count was not set & at least one processing were done => ok
   * 
   * @return
   * @throws InterruptedException
   */
  synchronized private boolean isProcessesCountCorrect() throws InterruptedException {
    boolean countReached = latch.await(timeout, TimeUnit.MILLISECONDS);
    if (needToMatchCount) {
      return count == invocationCount;
    } else {
      return countReached;
    }
  }
}
