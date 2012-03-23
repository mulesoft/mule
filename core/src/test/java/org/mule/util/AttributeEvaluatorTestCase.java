/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import org.hamcrest.core.Is;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.mule.api.expression.ExpressionManager;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

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
        assertThat(attributeEvaluator.isPlainText(), is(true));
        assertThat(attributeEvaluator.isRegularExpression(), is(false));
        assertThat(attributeEvaluator.isExpression(), is(false));
    }

    @Test
    public void testRegularExpressionValue()
    {
        String regexAttribute = "attributeEvaluator*";
        AttributeEvaluator attributeEvaluator = new AttributeEvaluator(regexAttribute);
        attributeEvaluator.enableRegexSupport();
        Mockito.when(mockExpressionManager.isExpression(regexAttribute)).thenReturn(false);
        attributeEvaluator.initialize(mockExpressionManager);
        assertThat(attributeEvaluator.isPlainText(), is(false));
        assertThat(attributeEvaluator.isRegularExpression(), is(true));
        assertThat(attributeEvaluator.isExpression(), is(false));
    }

    @Test
    public void testExpressionValue()
    {
        String attributeValue = "#[eval:express]";
        when(mockExpressionManager.isExpression(attributeValue)).thenReturn(true);
        Mockito.when(mockExpressionManager.isExpression(attributeValue)).thenReturn(true);
        AttributeEvaluator attributeEvaluator = new AttributeEvaluator(attributeValue);
        attributeEvaluator.initialize(mockExpressionManager);
        assertThat(attributeEvaluator.isPlainText(), is(false));
        assertThat(attributeEvaluator.isRegularExpression(), is(false));
        assertThat(attributeEvaluator.isExpression(), is(true));
    }

    @Test(expected = IllegalStateException.class)
    public void testNotRegularExpressionAndCallMatches()
    {
        String attributeValue = "#[eval:express]";
        when(mockExpressionManager.isExpression(attributeValue)).thenReturn(true);
        AttributeEvaluator attributeEvaluator = new AttributeEvaluator(attributeValue);
        attributeEvaluator.initialize(mockExpressionManager);
        attributeEvaluator.matches("some string");
    }

    @Test(expected = IllegalStateException.class)
    public void testNotRegularExpressionAndCallGetRegexPattern()
    {
        String attributeValue = "#[eval:express]";
        when(mockExpressionManager.isExpression(attributeValue)).thenReturn(true);
        AttributeEvaluator attributeEvaluator = new AttributeEvaluator(attributeValue);
        attributeEvaluator.initialize(mockExpressionManager);
        attributeEvaluator.getRegexPattern();
    }

    @Test
    public void testSimpleRegularExpression()
    {
        AttributeEvaluator attributeEvaluator = new AttributeEvaluator("*");
        attributeEvaluator.enableRegexSupport();
        attributeEvaluator.initialize(mockExpressionManager);
        assertThat(attributeEvaluator.matches("some"), is(true));
        assertThat(attributeEvaluator.matches("me"), is(true));
        assertThat(attributeEvaluator.matches("mean"), is(true));
    }
    
    @Test
    public void testSimpleRegularExpressionStartWith()
    {
        AttributeEvaluator attributeEvaluator = new AttributeEvaluator("*me");
        attributeEvaluator.enableRegexSupport();
        attributeEvaluator.initialize(mockExpressionManager);
        assertThat(attributeEvaluator.matches("some"), is(true));
        assertThat(attributeEvaluator.matches("me"), is(true));
        assertThat(attributeEvaluator.matches("mean"), is(false));
    }

    @Test
    public void testSimpleRegularExpressionEndsWith()
    {
        AttributeEvaluator attributeEvaluator = new AttributeEvaluator("me*");
        attributeEvaluator.enableRegexSupport();
        attributeEvaluator.initialize(mockExpressionManager);
        assertThat(attributeEvaluator.matches("some"), is(false));
        assertThat(attributeEvaluator.matches("me"), is(true));
        assertThat(attributeEvaluator.matches("mean"), is(true));
    }

    @Test
    public void testComplexRegularExpression()
    {
        AttributeEvaluator attributeEvaluator = new AttributeEvaluator(".*2$");
        attributeEvaluator.enableRegexSupport();
        attributeEvaluator.initialize(mockExpressionManager);
        assertThat(attributeEvaluator.matches("4352"), is(true));
        assertThat(attributeEvaluator.matches("noNumber"), is(false));
        assertThat(attributeEvaluator.matches("anything2"), is(true));
        assertThat(attributeEvaluator.matches("some2text"), is(false));
    }
}
