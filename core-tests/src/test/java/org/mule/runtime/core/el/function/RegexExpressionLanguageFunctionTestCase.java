/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.function;

import static java.util.Optional.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.core.TransformationService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.InternalMessage;
import org.mule.runtime.core.api.el.ExpressionExecutor;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.context.notification.DefaultFlowCallStack;
import org.mule.runtime.core.el.context.MessageContext;
import org.mule.runtime.core.el.mvel.MVELExpressionExecutor;
import org.mule.runtime.core.el.mvel.MVELExpressionLanguageContext;
import org.mule.mvel2.CompileException;
import org.mule.mvel2.ParserConfiguration;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Date;
import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class RegexExpressionLanguageFunctionTestCase extends AbstractMuleTestCase {

  protected ExpressionExecutor<MVELExpressionLanguageContext> expressionExecutor;
  protected MVELExpressionLanguageContext context;
  protected RegexExpressionLanguageFuntion regexFuntion;
  protected MuleContext muleContext;
  private Event event;
  private Event.Builder eventBuilder;
  private InternalMessage message;

  @Before
  public void setup() throws InitialisationException {
    ParserConfiguration parserConfiguration = new ParserConfiguration();
    expressionExecutor = new MVELExpressionExecutor(parserConfiguration);
    muleContext = mock(MuleContext.class);
    context = new MVELExpressionLanguageContext(parserConfiguration, muleContext);
    regexFuntion = new RegexExpressionLanguageFuntion();
    context.declareFunction("regex", regexFuntion);
  }

  @Test
  public void testReturnNullWhenDoesNotMatches() throws Exception {
    addMessageToContextWithPayload("TEST");
    Object result = regexFuntion.call(new Object[] {"'TESTw+TEST'"}, context);
    assertNull(result);
  }

  @Test
  public void testReturnNullWhenDoesNotMatchesMVEL() throws Exception {
    addMessageToContextWithPayload("TEST");
    Object result = expressionExecutor.execute("regex('TESTw+TEST')", context);
    assertNull(result);
  }

  @Test
  public void testReturnsPayloadWhenMatchesIfNoCaptureGroupDefined() throws Exception {
    addMessageToContextWithPayload("TESTfooTEST");
    Object result = regexFuntion.call(new Object[] {"TEST\\w+TEST"}, context);
    assertEquals("TESTfooTEST", result);
  }

  @Test
  public void testReturnsPayloadWhenMatchesIfNoCaptureGroupDefinedMVEL() throws Exception {
    addMessageToContextWithPayload("TESTfooTEST");
    Object result = expressionExecutor.execute("regex('TEST\\\\w+TEST')", context);
    assertEquals("TESTfooTEST", result);
  }

  @Test
  public void testReturnsMatchedValueIfCaptureGroupDefined() throws Exception {
    addMessageToContextWithPayload("TESTfooTEST");
    Object result = regexFuntion.call(new Object[] {"TEST(\\w+)TEST"}, context);
    assertEquals("foo", result);
  }

  @Test
  public void testReturnsMatchedValueIfCaptureGroupDefinedMVEL() throws Exception {
    addMessageToContextWithPayload("TESTfooTEST");
    Object result = expressionExecutor.execute("regex('TEST(\\\\w+)TEST')", context);
    assertEquals("foo", result);
  }

  @Test
  public void testReturnsMultipleValuesIfMultipleCaptureGroupDefine() throws Exception {
    addMessageToContextWithPayload("TESTfooTESTbar");
    Object result = regexFuntion.call(new Object[] {"TEST(\\w+)TEST(\\w+)"}, context);

    assertTrue(result instanceof String[]);
    String[] values = (String[]) result;
    assertEquals(2, values.length);
    assertEquals("foo", values[0]);
    assertEquals("bar", values[1]);
  }

  @Test
  public void testReturnsMultipleValuesIfMultipleCaptureGroupDefineMVEL() throws Exception {
    addMessageToContextWithPayload("TESTfooTESTbar");
    Object result = expressionExecutor.execute("regex('TEST(\\\\w+)TEST(\\\\w+)')", context);

    assertTrue(result instanceof String[]);
    String[] values = (String[]) result;
    assertEquals(2, values.length);
    assertEquals("foo", values[0]);
    assertEquals("bar", values[1]);
  }

  @Test
  public void testReturnsPayloadWhenMatchesIfNoCaptureGroupDefinedTextArgument() throws Exception {
    Object result = regexFuntion.call(new Object[] {"TEST\\w+TEST", "TESTfooTEST"}, context);
    assertEquals("TESTfooTEST", result);
  }

  @Test
  public void testReturnsPayloadWhenMatchesIfNoCaptureGroupDefinedTextArgumentMVEL() throws Exception {
    Object result = expressionExecutor.execute("regex('TEST\\\\w+TEST','TESTfooTEST')", context);
    assertEquals("TESTfooTEST", result);
  }

  @Test
  public void testReturnsPayloadWhenMatchesIfNoCaptureGroupDefinedTextAndFlagsArgument() throws Exception {
    Object result = regexFuntion.call(new Object[] {"test\\w+test", "TESTfooTEST", Pattern.CASE_INSENSITIVE}, context);
    assertEquals("TESTfooTEST", result);
  }

  @Test
  public void testReturnsPayloadWhenMatchesIfNoCaptureGroupDefinedTextAndFlagsArgumentMVEL() throws Exception {
    Object result =
        expressionExecutor.execute("regex('test\\\\w+test','TESTfooTEST', java.util.regex.Pattern.CASE_INSENSITIVE)", context);
    assertEquals("TESTfooTEST", result);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidNullRegex() throws Exception {
    regexFuntion.call(new Object[] {null}, context);
  }

  @Test(expected = CompileException.class)
  public void testInvalidNullRegexMVEL() throws Exception {
    expressionExecutor.execute("regex(null)", context);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidNonStringRegex() throws Exception {
    regexFuntion.call(new Object[] {new Date()}, context);
  }

  @Test(expected = CompileException.class)
  public void testInvalidNonStringRegexMVEL() throws Exception {
    expressionExecutor.execute("regex(new Date())", context);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidNullText() throws Exception {
    regexFuntion.call(new Object[] {"TESTw+TEST", null}, context);
  }

  @Test(expected = CompileException.class)
  public void testInvalidNullTextMVEL() throws Exception {
    expressionExecutor.execute("regex('TESTw+TEST',null)", context);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidNonStringText() throws Exception {
    regexFuntion.call(new Object[] {"TESTw+TEST", new Date()}, context);
  }

  @Test(expected = CompileException.class)
  public void testInvalidNonStringTextMVEL() throws Exception {
    expressionExecutor.execute("regex('TESTw+TEST',new Date())", context);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidNonIntFlags() throws Exception {
    regexFuntion.call(new Object[] {"TESTw+TEST", "text", "foo"}, context);
  }

  @Test(expected = CompileException.class)
  public void testInvalidNonIntFlagsMVEL() throws Exception {
    expressionExecutor.execute("regex('TESTw+TEST','text','foo')", context);
  }

  protected void addMessageToContextWithPayload(String payload) throws TransformerException {
    event = mock(Event.class);
    when(event.getFlowCallStack()).thenReturn(new DefaultFlowCallStack());
    when(event.getError()).thenReturn(empty());
    eventBuilder = Event.builder(event);
    message = mock(InternalMessage.class);
    when(event.getMessage()).thenAnswer(invocation -> message);
    InternalMessage transformedMessage = mock(InternalMessage.class);
    when(transformedMessage.getPayload()).thenReturn(payload);
    TransformationService transformationService = mock(TransformationService.class);
    when(muleContext.getTransformationService()).thenReturn(transformationService);
    when(transformationService.transform(any(InternalMessage.class), any(DataType.class))).thenReturn(transformedMessage);
    context.addFinalVariable("message", new MessageContext(event, eventBuilder, muleContext));
  }

}
