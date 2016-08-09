/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.tck;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;

import org.mule.functional.functional.ResponseAssertionMessageProcessor;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.processor.TestNonBlockingProcessor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ResponseAssertionMessageProcessorTestCase extends AssertionMessageProcessorTestCase {

  private TestNonBlockingProcessor testNonBlockingProcessor;

  @Before
  public void before() throws InitialisationException {
    testNonBlockingProcessor = new TestNonBlockingProcessor();
    testNonBlockingProcessor.initialise();
  }

  @After
  public void after() {
    testNonBlockingProcessor.dispose();
  }

  @Override
  protected ResponseAssertionMessageProcessor createAssertionMessageProcessor() {
    ResponseAssertionMessageProcessor mp = new ResponseAssertionMessageProcessor();
    mp.setListener(new SensingNullMessageProcessor());
    return mp;
  }

  @Test
  public void responseProcess() throws Exception {
    when(mockEvent.isAllowNonBlocking()).thenReturn(false);
    when(mockEvent.getExchangePattern()).thenReturn(MessageExchangePattern.REQUEST_RESPONSE);

    ResponseAssertionMessageProcessor asp = createAssertionMessageProcessor();
    asp.setListener(testNonBlockingProcessor);
    asp.setFlowConstruct(flowConstruct);
    asp.setExpression(TRUE_EXPRESSION);
    asp.setResponseExpression(TRUE_EXPRESSION);
    asp.setCount(1);
    asp.setResponseCount(1);
    asp.setResponseSameThread(false);
    asp.start();
    asp.process(mockEvent);
    assertFalse(asp.expressionFailed());
    assertFalse(asp.responseExpressionFailed());
    assertFalse(asp.countFailOrNullEvent());
    assertFalse(asp.responseCountFailOrNullEvent());
  }

  @Test
  public void responseProcessNonBlocking() throws Exception {
    when(mockEvent.isAllowNonBlocking()).thenReturn(true);
    when(mockEvent.getExchangePattern()).thenReturn(MessageExchangePattern.REQUEST_RESPONSE);

    ResponseAssertionMessageProcessor asp = createAssertionMessageProcessor();
    asp.setListener(testNonBlockingProcessor);
    asp.setFlowConstruct(flowConstruct);
    asp.setExpression(TRUE_EXPRESSION);
    asp.setResponseExpression(TRUE_EXPRESSION);
    asp.setCount(1);
    asp.setResponseCount(1);
    asp.setResponseSameThread(false);
    asp.start();
    asp.process(mockEvent);
    assertFalse(asp.expressionFailed());
    assertFalse(asp.responseExpressionFailed());
    assertFalse(asp.countFailOrNullEvent());
    assertFalse(asp.responseCountFailOrNullEvent());
  }

}
