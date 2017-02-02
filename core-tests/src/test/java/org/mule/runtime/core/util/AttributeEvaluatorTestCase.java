/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.metadata.DataType.fromObject;

import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class AttributeEvaluatorTestCase extends AbstractMuleTestCase {

  private ExtendedExpressionManager mockExpressionManager = mock(ExtendedExpressionManager.class);
  private Event mockMuleEvent = mock(Event.class);

  @Test
  public void plainTextValue() {
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator("attributeEvaluator");
    when(mockExpressionManager.isExpression("attributeEvaluator")).thenReturn(false);
    attributeEvaluator.initialize(mockExpressionManager);
    assertThat(attributeEvaluator.isParseExpression(), is(false));
    assertThat(attributeEvaluator.isExpression(), is(false));
  }

  @Test
  public void expressionValue() {
    String attributeValue = "#[mel:eval:express]";
    when(mockExpressionManager.isExpression(attributeValue)).thenReturn(true);
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator(attributeValue);
    attributeEvaluator.initialize(mockExpressionManager);
    assertThat(attributeEvaluator.isParseExpression(), is(false));
    assertThat(attributeEvaluator.isExpression(), is(true));
  }

  @Test
  public void expressionValueNoEvaluator() {
    String attributeValue = "#[mel:express]";
    when(mockExpressionManager.isExpression(attributeValue)).thenReturn(true);
    when(mockExpressionManager.isExpression(attributeValue)).thenReturn(true);
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator(attributeValue);
    attributeEvaluator.initialize(mockExpressionManager);
    assertThat(attributeEvaluator.isParseExpression(), is(false));
    assertThat(attributeEvaluator.isExpression(), is(true));
  }

  @Test
  public void parse() {
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator("1#[mel:2]3#[mel:4]5");
    attributeEvaluator.initialize(mockExpressionManager);
    assertThat(attributeEvaluator.isParseExpression(), is(true));
    assertThat(attributeEvaluator.isExpression(), is(false));
  }

  @Test
  public void testParseStartsWithExpression() {
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[mel:1]234#[mel:5]");
    attributeEvaluator.initialize(mockExpressionManager);
    assertThat(attributeEvaluator.isParseExpression(), is(true));
    assertThat(attributeEvaluator.isExpression(), is(false));
  }

  @Test
  public void parseStartsAndEndsWithExpression() {
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[mel:1]#[mel:2]");
    attributeEvaluator.initialize(mockExpressionManager);
    assertThat(attributeEvaluator.isParseExpression(), is(true));
    assertThat(attributeEvaluator.isExpression(), is(false));
  }

  @Test
  public void parenthesesInExpression() {
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[mel:(1)]");
    attributeEvaluator.initialize(mockExpressionManager);
    assertThat(attributeEvaluator.isParseExpression(), is(false));
    assertThat(attributeEvaluator.isExpression(), is(true));
  }

  @Test
  public void resolveStringWithObjectReturnValue() {
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[mel:expression]");
    attributeEvaluator.initialize(mockExpressionManager);
    final String expectedValue = "hi";
    Object value = new StringBuilder(expectedValue);
    doReturn(new TypedValue<>(value, fromObject(value))).when(mockExpressionManager).evaluate(anyString(), any(Event.class));
    assertThat(attributeEvaluator.resolveStringValue(mockMuleEvent), is(expectedValue));
  }

  @Test
  public void resolveIntegerWithNumericStringValue() {
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[mel:expression]");
    attributeEvaluator.initialize(mockExpressionManager);
    final String expectedValue = "123";
    doReturn(new TypedValue<>(expectedValue, fromObject(expectedValue))).when(mockExpressionManager).evaluate(anyString(),
                                                                                                              any(Event.class));
    assertThat(attributeEvaluator.resolveIntegerValue(mockMuleEvent), is(Integer.parseInt(expectedValue)));
  }

  @Test
  public void resolveIntegerWithNumericValue() {
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[mel:expression]");
    attributeEvaluator.initialize(mockExpressionManager);
    final long expectedValue = 1234l;
    doReturn(new TypedValue<>(expectedValue, fromObject(expectedValue))).when(mockExpressionManager).evaluate(anyString(),
                                                                                                              any(Event.class));
    assertThat(attributeEvaluator.resolveIntegerValue(mockMuleEvent), is((int) expectedValue));
  }

  @Test
  public void resolveBooleanWithBooleanStringValue() {
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[mel:expression]");
    attributeEvaluator.initialize(mockExpressionManager);
    final String expectedValue = "true";
    doReturn(new TypedValue<>(expectedValue, fromObject(expectedValue))).when(mockExpressionManager).evaluate(anyString(),
                                                                                                              any(Event.class));
    assertThat(attributeEvaluator.resolveBooleanValue(mockMuleEvent), is(Boolean.valueOf(expectedValue)));
  }

  @Test
  public void resolveBooleanWithBooleanValue() {
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[mel:expression]");
    attributeEvaluator.initialize(mockExpressionManager);
    final Boolean expectedValue = true;
    doReturn(new TypedValue<>(expectedValue, fromObject(expectedValue))).when(mockExpressionManager).evaluate(anyString(),
                                                                                                              any(Event.class));
    assertThat(attributeEvaluator.resolveBooleanValue(mockMuleEvent), is(Boolean.valueOf(expectedValue)));
  }

  @Test(expected = NumberFormatException.class)
  public void resolveIntegerWithNoNumericValue() {
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[mel:expression]");
    attributeEvaluator.initialize(mockExpressionManager);
    doReturn(new TypedValue<>("abcd", fromObject("abcd"))).when(mockExpressionManager).evaluate(anyString(), any(Event.class));
    attributeEvaluator.resolveIntegerValue(mockMuleEvent);
  }

  @Test
  public void nullAttributeValue() {
    final AttributeEvaluator nullAttributeEvaluator = new AttributeEvaluator(null);
    nullAttributeEvaluator.initialize(mockExpressionManager);
    assertThat(nullAttributeEvaluator.isExpression(), is(false));
    assertThat(nullAttributeEvaluator.isParseExpression(), is(false));
    assertThat(nullAttributeEvaluator.resolveValue(mockMuleEvent), nullValue());
    assertThat(nullAttributeEvaluator.resolveIntegerValue(mockMuleEvent), nullValue());
    assertThat(nullAttributeEvaluator.resolveStringValue(mockMuleEvent), nullValue());
  }

}
