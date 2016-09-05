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
import static org.mockito.Mockito.when;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.el.ExpressionLanguage;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class AttributeEvaluatorTestCase extends AbstractMuleTestCase {

  @Mock
  private ExpressionLanguage mockExpressionLanaguage;
  @Mock
  private MuleEvent mockMuleEvent;

  @Test
  public void plainTextValue() {
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator("attributeEvaluator");
    Mockito.when(mockExpressionLanaguage.isExpression("attributeEvaluator")).thenReturn(false);
    attributeEvaluator.initialize(mockExpressionLanaguage);
    assertThat(attributeEvaluator.isParseExpression(), is(false));
    assertThat(attributeEvaluator.isExpression(), is(false));
  }

  @Test
  public void expressionValue() {
    String attributeValue = "#[eval:express]";
    when(mockExpressionLanaguage.isExpression(attributeValue)).thenReturn(true);
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator(attributeValue);
    attributeEvaluator.initialize(mockExpressionLanaguage);
    assertThat(attributeEvaluator.isParseExpression(), is(false));
    assertThat(attributeEvaluator.isExpression(), is(true));
  }

  @Test
  public void expressionValueNoEvaluator() {
    String attributeValue = "#[express]";
    when(mockExpressionLanaguage.isExpression(attributeValue)).thenReturn(true);
    Mockito.when(mockExpressionLanaguage.isExpression(attributeValue)).thenReturn(true);
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator(attributeValue);
    attributeEvaluator.initialize(mockExpressionLanaguage);
    assertThat(attributeEvaluator.isParseExpression(), is(false));
    assertThat(attributeEvaluator.isExpression(), is(true));
  }

  @Test
  public void parse() {
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator("1#[2]3#[4]5");
    attributeEvaluator.initialize(mockExpressionLanaguage);
    assertThat(attributeEvaluator.isParseExpression(), is(true));
    assertThat(attributeEvaluator.isExpression(), is(false));
  }

  @Test
  public void testParseStartsWithExpression() {
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[1]234#[5]");
    attributeEvaluator.initialize(mockExpressionLanaguage);
    assertThat(attributeEvaluator.isParseExpression(), is(true));
    assertThat(attributeEvaluator.isExpression(), is(false));
  }

  @Test
  public void parseStartsAndEndsWithExpression() {
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[1]#[2]");
    attributeEvaluator.initialize(mockExpressionLanaguage);
    assertThat(attributeEvaluator.isParseExpression(), is(true));
    assertThat(attributeEvaluator.isExpression(), is(false));
  }

  @Test
  public void parenthesesInExpression() {
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[(1)]");
    attributeEvaluator.initialize(mockExpressionLanaguage);
    assertThat(attributeEvaluator.isParseExpression(), is(false));
    assertThat(attributeEvaluator.isExpression(), is(true));
  }

  @Test
  public void resolveStringWithObjectReturnValue() {
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[expression]");
    attributeEvaluator.initialize(mockExpressionLanaguage);
    final String expectedValue = "hi";
    when(mockExpressionLanaguage.evaluate(anyString(), any(MuleEvent.class), any(FlowConstruct.class)))
        .thenReturn(new StringBuilder(expectedValue));
    assertThat(attributeEvaluator.resolveStringValue(mockMuleEvent), is(expectedValue));
  }

  @Test
  public void resolveIntegerWithNumericStringValue() {
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[expression]");
    attributeEvaluator.initialize(mockExpressionLanaguage);
    final String expectedValue = "123";
    when(mockExpressionLanaguage.evaluate(anyString(), any(MuleEvent.class), any(FlowConstruct.class))).thenReturn(expectedValue);
    assertThat(attributeEvaluator.resolveIntegerValue(mockMuleEvent), is(Integer.parseInt(expectedValue)));
  }

  @Test
  public void resolveIntegerWithNumericValue() {
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[expression]");
    attributeEvaluator.initialize(mockExpressionLanaguage);
    final long expectedValue = 1234l;
    when(mockExpressionLanaguage.evaluate(anyString(), any(MuleEvent.class), any(FlowConstruct.class))).thenReturn(expectedValue);
    assertThat(attributeEvaluator.resolveIntegerValue(mockMuleEvent), is((int) expectedValue));
  }

  @Test
  public void resolveBooleanWithBooleanStringValue() {
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[expression]");
    attributeEvaluator.initialize(mockExpressionLanaguage);
    final String expectedValue = "true";
    when(mockExpressionLanaguage.evaluate(anyString(), any(MuleEvent.class), any(FlowConstruct.class))).thenReturn(expectedValue);
    assertThat(attributeEvaluator.resolveBooleanValue(mockMuleEvent), is(Boolean.valueOf(expectedValue)));
  }

  @Test
  public void resolveBooleanWithBooleanValue() {
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[expression]");
    attributeEvaluator.initialize(mockExpressionLanaguage);
    final Boolean expectedValue = true;
    when(mockExpressionLanaguage.evaluate(anyString(), any(MuleEvent.class), any(FlowConstruct.class))).thenReturn(expectedValue);
    assertThat(attributeEvaluator.resolveBooleanValue(mockMuleEvent), is(Boolean.valueOf(expectedValue)));
  }

  @Test(expected = NumberFormatException.class)
  public void resolveIntegerWithNoNumericValue() {
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[expression]");
    attributeEvaluator.initialize(mockExpressionLanaguage);
    final String value = "abcd";
    when(mockExpressionLanaguage.evaluate(anyString(), any(MuleEvent.class), any(FlowConstruct.class))).thenReturn(value);
    attributeEvaluator.resolveIntegerValue(mockMuleEvent);
  }

  @Test
  public void nullAttributeValue() {
    final AttributeEvaluator nullAttributeEvaluator = new AttributeEvaluator(null);
    nullAttributeEvaluator.initialize(mockExpressionLanaguage);
    assertThat(nullAttributeEvaluator.isExpression(), is(false));
    assertThat(nullAttributeEvaluator.isParseExpression(), is(false));
    assertThat(nullAttributeEvaluator.resolveValue(mockMuleEvent), nullValue());
    assertThat(nullAttributeEvaluator.resolveIntegerValue(mockMuleEvent), nullValue());
    assertThat(nullAttributeEvaluator.resolveStringValue(mockMuleEvent), nullValue());
  }

}
