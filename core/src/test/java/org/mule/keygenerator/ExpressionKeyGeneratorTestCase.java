/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
