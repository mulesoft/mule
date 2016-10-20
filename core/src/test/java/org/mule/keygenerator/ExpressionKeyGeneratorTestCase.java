/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.keygenerator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.expression.ExpressionManager;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.NotSerializableException;
import java.io.Serializable;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

public class ExpressionKeyGeneratorTestCase extends AbstractMuleTestCase
{

    private static final String RESOLVED_KEY = "KEY";
    private static final String SINGLE_EXPRESSION = "#[expression]";

    private ExpressionMuleEventKeyGenerator keyGenerator;
    private ExpressionManager expressionManager;
    private MuleContext muleContext;
    private MuleEvent event;

    @Before
    public void setUp() throws Exception
    {
        keyGenerator = new ExpressionMuleEventKeyGenerator();
        expressionManager = mock(ExpressionManager.class);
        muleContext = mock(MuleContext.class);
        when(muleContext.getExpressionManager()).thenReturn(expressionManager);
        event = mock(MuleEvent.class);

        MuleContext context = mock(MuleContext.class);
        when(context.getExpressionManager()).thenReturn(expressionManager);

        keyGenerator = new ExpressionMuleEventKeyGenerator();
    }

    @Test
    public void testGeneratesSerializableKey() throws Exception
    {
        keyGenerator.setExpression(SINGLE_EXPRESSION);
        keyGenerator.setMuleContext(muleContext);
        when(expressionManager.evaluate(SINGLE_EXPRESSION, event)).thenReturn(RESOLVED_KEY);

        Serializable key = keyGenerator.generateKey(event);

        assertThat(key, Matchers.<Serializable>equalTo(RESOLVED_KEY));
    }

    @Test
    public void resolvesCompositeExpression() throws Exception
    {
        keyGenerator.setExpression(SINGLE_EXPRESSION + SINGLE_EXPRESSION);
        keyGenerator.setMuleContext(muleContext);
        when(expressionManager.parse(SINGLE_EXPRESSION + SINGLE_EXPRESSION, event)).thenReturn(RESOLVED_KEY);

        Serializable key = keyGenerator.generateKey(event);

        assertThat(key, Matchers.<Serializable>equalTo(RESOLVED_KEY));
    }

    @Test(expected = NotSerializableException.class)
    public void testThrowsExceptionOnNonSerializableKey() throws Exception
    {
        keyGenerator.setExpression(SINGLE_EXPRESSION);
        keyGenerator.setMuleContext(muleContext);
        when(expressionManager.evaluate(SINGLE_EXPRESSION, event)).thenReturn(null);

        keyGenerator.generateKey(event);
    }
}
