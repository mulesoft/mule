/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.metadata.DataType.BOOLEAN;
import static org.mule.runtime.api.metadata.DataType.NUMBER;
import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.api.metadata.DataType.fromObject;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.privileged.util.AttributeEvaluator;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class MELAttributeEvaluatorTestCase extends AbstractMuleTestCase {

  private ExtendedExpressionManager mockExpressionManager = mock(ExtendedExpressionManager.class);
  private CoreEvent event = mock(CoreEvent.class);

  @Test
  public void plainTextValue() {
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator("attributeEvaluator");
    when(mockExpressionManager.isExpression("attributeEvaluator")).thenReturn(false);
    attributeEvaluator.initialize(mockExpressionManager);

    attributeEvaluator.resolveValue(event);
    verify(mockExpressionManager, never()).parse(anyString(), any(CoreEvent.class), any());
    verify(mockExpressionManager, never()).evaluate(anyString(), any(CoreEvent.class));
    verify(mockExpressionManager, never()).evaluate(anyString(), any(DataType.class), any(), any(CoreEvent.class));
  }

  @Test
  public void expressionValue() {
    String attributeValue = "#[mel:eval:express]";
    when(mockExpressionManager.evaluate(eq(attributeValue), any(CoreEvent.class))).thenReturn(new TypedValue(null, OBJECT));
    when(mockExpressionManager.isExpression(attributeValue)).thenReturn(true);
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator(attributeValue);
    attributeEvaluator.initialize(mockExpressionManager);

    attributeEvaluator.resolveValue(event);
    verify(mockExpressionManager, never()).parse(anyString(), any(CoreEvent.class), any());
    verify(mockExpressionManager).evaluate(anyString(), any(CoreEvent.class));
    verify(mockExpressionManager, never()).evaluate(anyString(), any(DataType.class), any(), any(CoreEvent.class));
  }

  @Test
  public void expressionValueNoEvaluator() {
    String attributeValue = "#[mel:express]";
    when(mockExpressionManager.evaluate(eq(attributeValue), any(CoreEvent.class))).thenReturn(new TypedValue(null, OBJECT));
    when(mockExpressionManager.isExpression(attributeValue)).thenReturn(true);
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator(attributeValue);
    attributeEvaluator.initialize(mockExpressionManager);

    attributeEvaluator.resolveValue(event);
    verify(mockExpressionManager, never()).parse(anyString(), any(CoreEvent.class), any());
    verify(mockExpressionManager).evaluate(anyString(), any(CoreEvent.class));
    verify(mockExpressionManager, never()).evaluate(anyString(), any(DataType.class), any(), any(CoreEvent.class));
  }

  @Test
  public void parse() {
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator("1#[mel:2]3#[mel:4]5");
    attributeEvaluator.initialize(mockExpressionManager);

    attributeEvaluator.resolveValue(event);
    verify(mockExpressionManager).parse(anyString(), any(CoreEvent.class), any());
    verify(mockExpressionManager, never()).evaluate(anyString(), any(CoreEvent.class));
    verify(mockExpressionManager, never()).evaluate(anyString(), any(DataType.class), any(), any(CoreEvent.class));

  }

  @Test
  public void testParseStartsWithExpression() {
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[mel:1]234#[mel:5]");
    attributeEvaluator.initialize(mockExpressionManager);

    attributeEvaluator.resolveValue(event);
    verify(mockExpressionManager).parse(anyString(), any(CoreEvent.class), any());
    verify(mockExpressionManager, never()).evaluate(anyString(), any(CoreEvent.class));
    verify(mockExpressionManager, never()).evaluate(anyString(), any(DataType.class), any(), any(CoreEvent.class));
  }

  @Test
  public void parseStartsAndEndsWithExpression() {
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[mel:1]#[mel:2]");
    attributeEvaluator.initialize(mockExpressionManager);

    attributeEvaluator.resolveValue(event);
    verify(mockExpressionManager).parse(anyString(), any(CoreEvent.class), any());
    verify(mockExpressionManager, never()).evaluate(anyString(), any(CoreEvent.class));
    verify(mockExpressionManager, never()).evaluate(anyString(), any(DataType.class), any(), any(CoreEvent.class));
  }

  @Test
  public void parenthesesInExpression() {
    when(mockExpressionManager.evaluate(anyString(), any(CoreEvent.class))).thenReturn(new TypedValue(null, OBJECT));
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[mel:(1)]");
    attributeEvaluator.initialize(mockExpressionManager);

    attributeEvaluator.resolveValue(event);
    verify(mockExpressionManager, never()).parse(anyString(), any(CoreEvent.class), any());
    verify(mockExpressionManager).evaluate(anyString(), any(CoreEvent.class));
    verify(mockExpressionManager, never()).evaluate(anyString(), any(DataType.class), any(), any(CoreEvent.class));
  }

  @Test
  public void resolveIntegerWithNumericStringValue() {
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[mel:expression]", NUMBER);
    attributeEvaluator.initialize(mockExpressionManager);
    final String expectedValue = "123";
    doReturn(new TypedValue<>(Integer.parseInt(expectedValue), NUMBER))
        .when(mockExpressionManager)
        .evaluate(anyString(), any(DataType.class), any(BindingContext.class), any(CoreEvent.class));
    assertThat(attributeEvaluator.resolveValue(event), is(Integer.parseInt(expectedValue)));
  }

  @Test
  public void resolveIntegerWithNumericValue() {
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[mel:expression]", NUMBER);
    attributeEvaluator.initialize(mockExpressionManager);
    final long expectedValue = 1234l;
    doReturn(new TypedValue<>(expectedValue, fromObject(expectedValue)))
        .when(mockExpressionManager)
        .evaluate(anyString(), any(DataType.class), any(BindingContext.class), any(CoreEvent.class));
    assertThat(attributeEvaluator.resolveValue(event), is(expectedValue));
  }

  @Test
  public void resolveBooleanWithBooleanStringValue() {
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[mel:expression]", BOOLEAN);
    attributeEvaluator.initialize(mockExpressionManager);
    final String expectedValue = "true";
    doReturn(new TypedValue<>(Boolean.valueOf(expectedValue), BOOLEAN))
        .when(mockExpressionManager)
        .evaluate(anyString(), any(DataType.class), any(BindingContext.class), any(CoreEvent.class));
    assertThat(attributeEvaluator.resolveValue(event), is(Boolean.valueOf(expectedValue)));
  }

  @Test
  public void resolveBooleanWithBooleanValue() {
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[mel:expression]", BOOLEAN);
    attributeEvaluator.initialize(mockExpressionManager);
    final Boolean expectedValue = true;
    doReturn(new TypedValue<>(expectedValue, fromObject(expectedValue)))
        .when(mockExpressionManager)
        .evaluate(anyString(), any(DataType.class), any(BindingContext.class), any(CoreEvent.class));
    assertThat(attributeEvaluator.resolveValue(event), is(Boolean.valueOf(expectedValue)));
  }

  @Test(expected = ExpressionRuntimeException.class)
  public void resolveIntegerWithNoNumericValue() {
    AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[mel:expression]", NUMBER);
    attributeEvaluator.initialize(mockExpressionManager);
    doThrow(ExpressionRuntimeException.class).when(mockExpressionManager)
        .evaluate(anyString(), any(DataType.class), any(BindingContext.class), any(CoreEvent.class));
    attributeEvaluator.resolveValue(event);
  }

  @Test
  public void nullAttributeValue() {
    final AttributeEvaluator nullAttributeEvaluator = new AttributeEvaluator(null, OBJECT);
    nullAttributeEvaluator.initialize(mockExpressionManager);
    doReturn(new TypedValue<>(null, OBJECT))
        .when(mockExpressionManager)
        .evaluate(anyString(), any(DataType.class), any(BindingContext.class), any(CoreEvent.class));

    assertThat(nullAttributeEvaluator.resolveValue(event), nullValue());
    verify(mockExpressionManager, never()).parse(anyString(), any(CoreEvent.class), any());
    verify(mockExpressionManager, never()).evaluate(anyString(), any(CoreEvent.class));
    verify(mockExpressionManager, never()).evaluate(anyString(), any(DataType.class), any(), any(CoreEvent.class));
  }

}
