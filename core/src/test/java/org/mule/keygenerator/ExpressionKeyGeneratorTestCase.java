/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.keygenerator;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionManager;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.NotSerializableException;
import java.io.Serializable;

import org.junit.Before;
import org.junit.Test;

public class ExpressionKeyGeneratorTestCase extends AbstractMuleTestCase
{

    private static final String KEY = "KEY";
    private static final String EXPRESSION = "muleExpression";

    private ExpressionMuleEventKeyGenerator keyGenerator;
    private MuleMessage message;
    private MuleEvent event;

    @Before
    public void setUp() throws Exception
    {
        expressionManager = mock(ExpressionManager.class);
        MuleContext context = mock(MuleContext.class);
        when(context.getExpressionManager()).thenReturn(expressionManager);

        message = mock(MuleMessage.class);

        event = mock(MuleEvent.class);
        when(event.getMessage()).thenReturn(message);
        when(event.getMuleContext()).thenReturn(context);

        keyGenerator = new ExpressionMuleEventKeyGenerator();
        keyGenerator.setExpression(EXPRESSION);
    }

    private ExpressionManager expressionManager;

    @Test
    public void testGeneratesSerializableKey() throws Exception
    {
        when(expressionManager.evaluate(EXPRESSION, event)).thenReturn(KEY);
        Serializable key = keyGenerator.generateKey(event);

        assertEquals(KEY, key);
    }

    @Test(expected = NotSerializableException.class)
    public void testThrowsExceptionOnNonSerializableKey() throws Exception
    {
        when(expressionManager.evaluate(EXPRESSION, event)).thenReturn(null);
        keyGenerator.generateKey(event);
    }
}
