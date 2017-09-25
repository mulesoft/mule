/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.tck;

import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.el.ValidationResult.success;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;

import org.mule.functional.api.component.AssertionMessageProcessor;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AssertionMessageProcessorTestCase extends AbstractMuleTestCase {

  protected FlowConstruct flowConstruct;
  protected ExtendedExpressionManager expressionManager;
  protected final String TRUE_EXPRESSION = "trueExpression";
  protected final String FALSE_EXPRESSION = "falseExpression";

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  protected MuleContext muleContext;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  protected CoreEvent mockEvent;

  @Mock
  protected InternalMessage muleMessage;

  @Before
  public void initialise() {
    when(mockEvent.getMessage()).thenReturn(muleMessage);
    expressionManager = mock(ExtendedExpressionManager.class);
    when(expressionManager.isValid(anyString())).thenReturn(true);
    when(expressionManager.validate(anyString())).thenReturn(success());
    when(expressionManager.evaluateBoolean(eq(TRUE_EXPRESSION), any(CoreEvent.class), any(ComponentLocation.class),
                                           anyBoolean(),
                                           anyBoolean()))
                                               .thenReturn(true);
    when(expressionManager.evaluateBoolean(eq(FALSE_EXPRESSION), any(CoreEvent.class), any(ComponentLocation.class),
                                           anyBoolean(),
                                           anyBoolean()))
                                               .thenReturn(false);

    when(muleContext.getExpressionManager()).thenReturn(expressionManager);

    flowConstruct = mock(FlowConstruct.class);
    when(flowConstruct.getMuleContext()).thenReturn(muleContext);
    when(flowConstruct.getName()).thenReturn("MockedFlowConstruct");
  }

  @Test
  public void startAssertionMessageProcessor() throws Exception {
    AssertionMessageProcessor asp = baseAssertionMP();
    asp.start();
  }

  @Test
  public void processDummyEvent() throws Exception {
    AssertionMessageProcessor asp = baseAssertionMP();
    asp.start();
    asp.process(mockEvent);
  }

  @Test
  public void processValidEvent() throws Exception {
    AssertionMessageProcessor asp = baseAssertionMP();
    asp.setExpression(TRUE_EXPRESSION);
    asp.start();
    asp.process(mockEvent);
    assertThat(asp.expressionFailed(), is(false));
    assertThat(asp.countFailOrNullEvent(), is(false));
  }

  @Test
  public void processInvalidEvent() throws Exception {
    AssertionMessageProcessor asp = baseAssertionMP();
    asp.setExpression(FALSE_EXPRESSION);
    asp.start();
    asp.process(mockEvent);
    assertThat(asp.expressionFailed(), is(true));
    assertThat(asp.countFailOrNullEvent(), is(false));
  }

  @Test
  public void processZeroEvents() throws Exception {
    AssertionMessageProcessor asp = baseAssertionMP();
    asp.setExpression(TRUE_EXPRESSION);
    asp.start();
    assertThat(asp.expressionFailed(), is(false));
    assertThat(asp.countFailOrNullEvent(), is(true));
  }

  @Test
  public void processSomeValidEvents() throws Exception {
    AssertionMessageProcessor asp = baseAssertionMP();
    asp.setExpression(TRUE_EXPRESSION);
    asp.start();
    asp.process(mockEvent);
    asp.process(mockEvent);
    asp.process(mockEvent);
    assertThat(asp.expressionFailed(), is(false));
    assertThat(asp.countFailOrNullEvent(), is(false));
  }

  @Test
  public void processSomeInvalidEvent() throws Exception {
    AssertionMessageProcessor asp = baseAssertionMP();
    asp.setExpression(TRUE_EXPRESSION);
    asp.start();
    asp.process(mockEvent);
    asp.process(mockEvent);
    asp.setExpression(FALSE_EXPRESSION);
    asp.process(mockEvent);
    asp.setExpression(TRUE_EXPRESSION);
    asp.process(mockEvent);
    assertThat(asp.expressionFailed(), is(true));
    assertThat(asp.countFailOrNullEvent(), is(false));
  }

  @Test
  public void processMoreThanCountEvents() throws Exception {
    AssertionMessageProcessor asp = baseAssertionMP();
    asp.setExpression(TRUE_EXPRESSION);
    asp.setCount(5);
    asp.start();
    for (int i = 0; i < 6; i++) {
      asp.process(mockEvent);
    }
    assertThat(asp.expressionFailed(), is(false));
    assertThat(asp.countFailOrNullEvent(), is(true));
  }

  @Test
  public void processLessThanCountEvents() throws Exception {
    AssertionMessageProcessor asp = baseAssertionMP();
    asp.setExpression(TRUE_EXPRESSION);
    asp.setCount(5);
    asp.start();
    for (int i = 0; i < 4; i++) {
      asp.process(mockEvent);
    }
    assertThat(asp.expressionFailed(), is(false));
    assertThat(asp.countFailOrNullEvent(), is(true));
  }

  @Test
  public void processExactCountEvents() throws Exception {
    AssertionMessageProcessor asp = baseAssertionMP();
    asp.setExpression(TRUE_EXPRESSION);
    asp.setCount(5);
    asp.start();
    for (int i = 0; i < 5; i++) {
      asp.process(mockEvent);
    }
    assertThat(asp.expressionFailed(), is(false));
    assertThat(asp.countFailOrNullEvent(), is(false));
  }

  public AssertionMessageProcessor baseAssertionMP() {
    AssertionMessageProcessor asp = createAssertionMessageProcessor();
    asp.setExpressionManager(expressionManager);
    asp.setAnnotations(singletonMap(LOCATION_KEY, fromSingleComponent("flow")));
    return asp;
  }

  protected AssertionMessageProcessor createAssertionMessageProcessor() {
    return new AssertionMessageProcessor();
  }

}
