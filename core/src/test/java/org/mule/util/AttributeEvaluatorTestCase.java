/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

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

    @Test
    public void testPlainTextValue()
    {
        AttributeEvaluator attributeEvaluator = new AttributeEvaluator("attributeEvaluator");
        Mockito.when(mockExpressionManager.isExpression("attributeEvaluator")).thenReturn(false);
        attributeEvaluator.initialize(mockExpressionManager);
        assertThat(attributeEvaluator.isString(), is(true));
        assertThat(attributeEvaluator.isExpression(), is(false));
    }

    @Test
    public void testExpressionValue()
    {
        String attributeValue = "#[eval:express]";
        when(mockExpressionManager.isExpression(attributeValue)).thenReturn(true);
        AttributeEvaluator attributeEvaluator = new AttributeEvaluator(attributeValue);
        attributeEvaluator.initialize(mockExpressionManager);
        assertThat(attributeEvaluator.isString(), is(false));
        assertThat(attributeEvaluator.isExpression(), is(true));
    }

    @Test
    public void testExpressionValueNoEvaluator()
    {
        String attributeValue = "#[express]";
        when(mockExpressionManager.isExpression(attributeValue)).thenReturn(true);
        Mockito.when(mockExpressionManager.isExpression(attributeValue)).thenReturn(true);
        AttributeEvaluator attributeEvaluator = new AttributeEvaluator(attributeValue);
        attributeEvaluator.initialize(mockExpressionManager);
        assertThat(attributeEvaluator.isString(), is(false));
        assertThat(attributeEvaluator.isExpression(), is(true));
    }

    @Test
    public void testParse()
    {
        AttributeEvaluator attributeEvaluator = new AttributeEvaluator("1#[2]3#[4]5");
        attributeEvaluator.initialize(mockExpressionManager);
        assertThat(attributeEvaluator.isString(), is(true));
        assertThat(attributeEvaluator.isExpression(), is(false));
    }

    @Test
    public void testParseStartsWithExpression()
    {
        AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[1]234#[5]");
        attributeEvaluator.initialize(mockExpressionManager);
        assertThat(attributeEvaluator.isString(), is(true));
        assertThat(attributeEvaluator.isExpression(), is(false));
    }

    @Test
    public void testParseStartsAndEndsWithExpression()
    {
        AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[1]#[2]");
        attributeEvaluator.initialize(mockExpressionManager);
        assertThat(attributeEvaluator.isString(), is(true));
        assertThat(attributeEvaluator.isExpression(), is(false));
    }

    @Test
    public void testParenthesesInExpression()
    {
        AttributeEvaluator attributeEvaluator = new AttributeEvaluator("#[(1)]");
        attributeEvaluator.initialize(mockExpressionManager);
        assertThat(attributeEvaluator.isString(), is(false));
        assertThat(attributeEvaluator.isExpression(), is(true));
    }

}
