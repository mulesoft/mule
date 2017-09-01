/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.tck;

import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;

import org.mule.functional.api.component.ResponseAssertionMessageProcessor;
import org.mule.functional.api.component.TestNonBlockingProcessor;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.tck.SensingNullMessageProcessor;

import org.junit.Before;
import org.junit.Test;

public class ResponseAssertionMessageProcessorTestCase extends AssertionMessageProcessorTestCase {

  private TestNonBlockingProcessor testNonBlockingProcessor;

  @Before
  public void before() throws InitialisationException {
    testNonBlockingProcessor = new TestNonBlockingProcessor();
  }

  @Override
  protected ResponseAssertionMessageProcessor createAssertionMessageProcessor() {
    ResponseAssertionMessageProcessor mp = new ResponseAssertionMessageProcessor();
    mp.setListener(new SensingNullMessageProcessor());
    return mp;
  }

  @Test
  public void responseProcess() throws Exception {
    ResponseAssertionMessageProcessor asp = createAssertionMessageProcessor();
    asp.setListener(testNonBlockingProcessor);
    asp.setExpressionManager(expressionManager);
    asp.setAnnotations(singletonMap(LOCATION_KEY, fromSingleComponent("flow")));
    asp.setExpression(TRUE_EXPRESSION);
    asp.setResponseExpression(TRUE_EXPRESSION);
    asp.setCount(1);
    asp.setResponseCount(1);
    asp.setResponseSameTask(false);
    asp.start();
    asp.process(mockEvent);
    assertFalse(asp.expressionFailed());
    assertFalse(asp.responseExpressionFailed());
    assertFalse(asp.countFailOrNullEvent());
    assertFalse(asp.responseCountFailOrNullEvent());
  }

  @Test
  public void responseProcessNonBlocking() throws Exception {
    when(mockEvent.getError()).thenReturn(empty());

    ResponseAssertionMessageProcessor asp = createAssertionMessageProcessor();
    asp.setListener(testNonBlockingProcessor);
    asp.setExpressionManager(expressionManager);
    asp.setAnnotations(singletonMap(LOCATION_KEY, fromSingleComponent("flow")));
    asp.setExpression(TRUE_EXPRESSION);
    asp.setResponseExpression(TRUE_EXPRESSION);
    asp.setCount(1);
    asp.setResponseCount(1);
    asp.setResponseSameTask(false);
    asp.start();
    asp.process(mockEvent);
    assertFalse(asp.expressionFailed());
    assertFalse(asp.responseExpressionFailed());
    assertFalse(asp.countFailOrNullEvent());
    assertFalse(asp.responseCountFailOrNullEvent());
  }

}
