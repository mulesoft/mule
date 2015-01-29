/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.tck;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.expression.ExpressionManager;
import org.mule.expression.DefaultExpressionManager;
import org.mule.tck.functional.AssertionMessageProcessor;
import org.mule.tck.junit4.AbstractMuleTestCase;

public class AssertionMessageProcessorTestCase extends AbstractMuleTestCase
{
    private FlowConstruct flowConstruct;
    private ExpressionManager expressionManager;
    private MuleContext muleContext;
    private final String TRUE_EXPRESSION = "trueExpression";
    private final String FALSE_EXPRESSION = "falseExpression";

    @Before
    public void initialise()
    {
        expressionManager = mock(DefaultExpressionManager.class);
        when(expressionManager.isValidExpression(anyString())).thenReturn(true);
        when(expressionManager.evaluateBoolean(eq(TRUE_EXPRESSION), any(MuleEvent.class), anyBoolean(), anyBoolean()))
            .thenReturn(true);
        when(expressionManager.evaluateBoolean(eq(FALSE_EXPRESSION), any(MuleEvent.class), anyBoolean(), anyBoolean()))
            .thenReturn(false);

        muleContext = mock(MuleContext.class);
        when(muleContext.getExpressionManager()).thenReturn(expressionManager);

        flowConstruct = mock(FlowConstruct.class);
        when(flowConstruct.getMuleContext()).thenReturn(muleContext);
        when(flowConstruct.getName()).thenReturn("MockedFlowConstruct");
    }

    @Test
    public void startAssertionMessageProcessor() throws Exception
    {
        AssertionMessageProcessor asp = new AssertionMessageProcessor();
        asp.setFlowConstruct(flowConstruct);
        asp.start();
    }

    @Test
    public void processDummyEvent() throws Exception
    {
        AssertionMessageProcessor asp = new AssertionMessageProcessor();
        asp.setFlowConstruct(flowConstruct);
        asp.start();
        asp.process(mock(MuleEvent.class));
    }

    @Test
    public void processValidEvent() throws Exception
    {
        AssertionMessageProcessor asp = new AssertionMessageProcessor();
        asp.setFlowConstruct(flowConstruct);
        asp.setExpression(TRUE_EXPRESSION);
        asp.start();
        asp.process(mock(MuleEvent.class));
        assertFalse(asp.expressionFailed());
        assertFalse(asp.countFailOrNullEvent());
    }

    @Test
    public void processInvalidEvent() throws Exception
    {
        AssertionMessageProcessor asp = new AssertionMessageProcessor();
        asp.setFlowConstruct(flowConstruct);
        asp.setExpression(FALSE_EXPRESSION);
        asp.start();
        asp.process(mock(MuleEvent.class));
        assertTrue(asp.expressionFailed());
        assertFalse(asp.countFailOrNullEvent());
    }

    @Test
    public void processZeroEvents() throws Exception
    {
        AssertionMessageProcessor asp = new AssertionMessageProcessor();
        asp.setFlowConstruct(flowConstruct);
        asp.setExpression(TRUE_EXPRESSION);
        asp.start();
        assertFalse(asp.expressionFailed());
        assertTrue(asp.countFailOrNullEvent());
    }

    @Test
    public void processSomeValidEvents() throws Exception
    {
        AssertionMessageProcessor asp = new AssertionMessageProcessor();
        asp.setFlowConstruct(flowConstruct);
        asp.setExpression(TRUE_EXPRESSION);
        asp.start();
        asp.process(mock(MuleEvent.class));
        asp.process(mock(MuleEvent.class));
        asp.process(mock(MuleEvent.class));
        assertFalse(asp.expressionFailed());
        assertFalse(asp.countFailOrNullEvent());
    }

    @Test
    public void processSomeInvalidEvent() throws Exception
    {
        AssertionMessageProcessor asp = new AssertionMessageProcessor();
        asp.setFlowConstruct(flowConstruct);
        asp.setExpression(TRUE_EXPRESSION);
        asp.start();
        asp.process(mock(MuleEvent.class));
        asp.process(mock(MuleEvent.class));
        asp.setExpression(FALSE_EXPRESSION);
        asp.process(mock(MuleEvent.class));
        asp.setExpression(TRUE_EXPRESSION);
        asp.process(mock(MuleEvent.class));
        assertTrue(asp.expressionFailed());
        assertFalse(asp.countFailOrNullEvent());
    }

    @Test
    public void processMoreThanCountEvents() throws Exception
    {
        AssertionMessageProcessor asp = new AssertionMessageProcessor();
        asp.setFlowConstruct(flowConstruct);
        asp.setExpression(TRUE_EXPRESSION);
        asp.setCount(5);
        asp.start();
        for(int i = 0; i < 6; i++)
        {
            asp.process(mock(MuleEvent.class));
        }
        assertFalse(asp.expressionFailed());
        assertTrue(asp.countFailOrNullEvent());
    }

    @Test
    public void processLessThanCountEvents() throws Exception
    {
        AssertionMessageProcessor asp = new AssertionMessageProcessor();
        asp.setFlowConstruct(flowConstruct);
        asp.setExpression(TRUE_EXPRESSION);
        asp.setCount(5);
        asp.start();
        for(int i = 0; i < 4; i++)
        {
            asp.process(mock(MuleEvent.class));
        }
        assertFalse(asp.expressionFailed());
        assertTrue(asp.countFailOrNullEvent());
    }

    @Test
    public void processExactCountEvents() throws Exception
    {
        AssertionMessageProcessor asp = new AssertionMessageProcessor();
        asp.setFlowConstruct(flowConstruct);
        asp.setExpression(TRUE_EXPRESSION);
        asp.setCount(5);
        asp.start();
        for(int i = 0; i < 5; i++)
        {
            asp.process(mock(MuleEvent.class));
        }
        assertFalse(asp.expressionFailed());
        assertFalse(asp.countFailOrNullEvent());
    }

    @Test
    public void processNullEvent() throws Exception
    {
        AssertionMessageProcessor asp = new AssertionMessageProcessor();
        asp.setFlowConstruct(flowConstruct);
        asp.setExpression(TRUE_EXPRESSION);
        asp.setCount(5);
        asp.start();
        asp.process(null);
        assertFalse(asp.expressionFailed());
        assertTrue(asp.countFailOrNullEvent());
    }
}
