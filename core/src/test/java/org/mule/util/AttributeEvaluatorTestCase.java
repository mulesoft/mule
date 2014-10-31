/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import org.mule.api.MuleEvent;
import org.mule.api.expression.ExpressionManager;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class AttributeEvaluatorTestCase extends AbstractMuleTestCase
{

    @Mock
    private ExpressionManager mockExpressionManager;
    @Mock
    private MuleEvent mockMuleEvent;

    @Test
    public void plainTextValue()
    {
        AttributeEvaluator attributeEvaluator = new AttributeEvaluator("attributeEvaluator");
        Mockito.when(mockExpressionManager.isExpression("attributeEvaluator")).thenReturn(false);
        attributeEvaluator.initialize(mockExpressionManager);
        assertThat(attributeEvaluator.isParseExpression(), is(false));
        assertThat(attributeEvaluator.isExpression(), is(false));
    }

    @Test
    public void expressionValue()
    {
        String attributeValue = "#[eval:express]";
        when(mockExpressionManager.isExpression(attributeValue)).thenReturn(true);
        AttributeEvaluator attributeEvaluator = new AttributeEvaluator(attributeValue);
        attributeEvaluator.initialize(mockExpressionManager);
        assertThat(attributeEvaluator.isParseExpression(), is(false));
        assertThat(attributeEvaluator.isExpression(), is(true));
    }

    @Test
    public void expressionValueNoEvaluator()
    {
        String attributeValue = "#[express]";
        when(mockExpressionManager.isExpression(attributeValue)).thenReturn(true);
        Mockito.when(mockExpressionManager.isExpression(attributeValue)).thenReturn(true);
        AttributeEvaluator attributeEvaluator = new AttributeEvaluator(attributeValue);
        attributeEvaluator.initialize(mockExpressionManager);
        assertThat(attributeEvaluator.isParseExpression(), is(false));
        assertThat(attributeEvaluator.isExpression(), is(true));
    }

    @Test
    public void parse()
    {
        AttributeEvaluator attributeEvaluator = new AttributeEvaluator("1#[2]3#[4]5");
        attributeEvaluator.initialize(mockExpressionManager);
        assertThat(attributeEvaluator.isParseExpression(), is(true));
        assertThat(attributeEvaluator.isExpression(), is(false));
    }

    @Test
    public void testParseStartsWithExpression()
    {
        AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[1]234#[5]");
        attributeEvaluator.initialize(mockExpressionManager);
        assertThat(attributeEvaluator.isParseExpression(), is(true));
        assertThat(attributeEvaluator.isExpression(), is(false));
    }

    @Test
    public void parseStartsAndEndsWithExpression()
    {
        AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[1]#[2]");
        attributeEvaluator.initialize(mockExpressionManager);
        assertThat(attributeEvaluator.isParseExpression(), is(true));
        assertThat(attributeEvaluator.isExpression(), is(false));
    }

    @Test
    public void parenthesesInExpression()
    {
        AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[(1)]");
        attributeEvaluator.initialize(mockExpressionManager);
        assertThat(attributeEvaluator.isParseExpression(), is(false));
        assertThat(attributeEvaluator.isExpression(), is(true));
    }

    @Test
    public void resolveStringWithObjectReturnValue()
    {
        AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[expression]");
        attributeEvaluator.initialize(mockExpressionManager);
        final String expectedValue = "hi";
        when(mockExpressionManager.evaluate(Mockito.anyString(), Mockito.any(MuleEvent.class))).thenReturn(new StringBuilder(expectedValue));
        assertThat(attributeEvaluator.resolveStringValue(mockMuleEvent), is(expectedValue));
    }

    @Test
    public void resolveIntegerWithNumericStringValue()
    {
        AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[expression]");
        attributeEvaluator.initialize(mockExpressionManager);
        final String expectedValue = "123";
        when(mockExpressionManager.evaluate(Mockito.anyString(), Mockito.any(MuleEvent.class))).thenReturn(expectedValue);
        assertThat(attributeEvaluator.resolveIntegerValue(mockMuleEvent), is(Integer.parseInt(expectedValue)));
    }

    @Test
    public void resolveIntegerWithNumericValue()
    {
        AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[expression]");
        attributeEvaluator.initialize(mockExpressionManager);
        final long expectedValue = 1234l;
        when(mockExpressionManager.evaluate(Mockito.anyString(), Mockito.any(MuleEvent.class))).thenReturn(expectedValue);
        assertThat(attributeEvaluator.resolveIntegerValue(mockMuleEvent), is((int)expectedValue));
    }

    @Test
    public void resolveBooleanWithBooleanStringValue()
    {
        AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[expression]");
        attributeEvaluator.initialize(mockExpressionManager);
        final String expectedValue = "true";
        when(mockExpressionManager.evaluate(Mockito.anyString(), Mockito.any(MuleEvent.class))).thenReturn(expectedValue);
        assertThat(attributeEvaluator.resolveBooleanValue(mockMuleEvent), is(Boolean.valueOf(expectedValue)));
    }

    @Test
    public void resolveBooleanWithBooleanValue()
    {
        AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[expression]");
        attributeEvaluator.initialize(mockExpressionManager);
        final Boolean expectedValue = true;
        when(mockExpressionManager.evaluate(Mockito.anyString(), Mockito.any(MuleEvent.class))).thenReturn(expectedValue);
        assertThat(attributeEvaluator.resolveBooleanValue(mockMuleEvent), is(Boolean.valueOf(expectedValue)));
    }

    @Test(expected = NumberFormatException.class)
    public void resolveIntegerWithNoNumericValue()
    {
        AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[expression]");
        attributeEvaluator.initialize(mockExpressionManager);
        final String value = "abcd";
        when(mockExpressionManager.evaluate(Mockito.anyString(), Mockito.any(MuleEvent.class))).thenReturn(value);
        attributeEvaluator.resolveIntegerValue(mockMuleEvent);
    }

    @Test
    public void nullAttributeValue()
    {
        final AttributeEvaluator nullAttributeEvaluator = new AttributeEvaluator(null);
        nullAttributeEvaluator.initialize(mockExpressionManager);
        assertThat(nullAttributeEvaluator.isExpression(), is(false));
        assertThat(nullAttributeEvaluator.isParseExpression(), is(false));
        assertThat(nullAttributeEvaluator.resolveValue(mockMuleEvent), nullValue());
        assertThat(nullAttributeEvaluator.resolveIntegerValue(mockMuleEvent), nullValue());
        assertThat(nullAttributeEvaluator.resolveStringValue(mockMuleEvent), nullValue());
    }

}
