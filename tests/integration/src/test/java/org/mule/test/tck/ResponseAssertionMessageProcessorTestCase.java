/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.tck;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.expression.ExpressionManager;
import org.mule.expression.DefaultExpressionManager;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.functional.AssertionMessageProcessor;
import org.mule.tck.functional.ResponseAssertionMessageProcessor;
import org.mule.tck.functional.TestNonBlockingProcessor;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Test;

public class ResponseAssertionMessageProcessorTestCase extends AssertionMessageProcessorTestCase
{
    protected ResponseAssertionMessageProcessor createAssertionMessageProcessor()
    {
        ResponseAssertionMessageProcessor mp = new ResponseAssertionMessageProcessor();
        mp.setListener(new SensingNullMessageProcessor());
        return mp;
    }

    @Test
    public void responseProcess() throws Exception
    {
        when(mockEvent.isAllowNonBlocking()).thenReturn(false);
        when(mockEvent.getExchangePattern()).thenReturn(MessageExchangePattern.REQUEST_RESPONSE);

        ResponseAssertionMessageProcessor asp = createAssertionMessageProcessor();
        asp.setListener(new TestNonBlockingProcessor());
        asp.setFlowConstruct(flowConstruct);
        asp.setExpression(TRUE_EXPRESSION);
        asp.setResponseExpression(TRUE_EXPRESSION);
        asp.setCount(1);
        asp.setResponseCount(1);
        asp.setResponseSameThread(false);
        asp.start();
        asp.process(mockEvent);
        assertFalse(asp.expressionFailed());
        assertFalse(asp.responseExpressionFailed());
        assertFalse(asp.countFailOrNullEvent());
        assertFalse(asp.responseCountFailOrNullEvent());
    }

    @Test
    public void responseProcessNonBlocking() throws Exception
    {
        when(mockEvent.isAllowNonBlocking()).thenReturn(true);
        when(mockEvent.getExchangePattern()).thenReturn(MessageExchangePattern.REQUEST_RESPONSE);

        ResponseAssertionMessageProcessor asp = createAssertionMessageProcessor();
        asp.setListener(new TestNonBlockingProcessor());
        asp.setFlowConstruct(flowConstruct);
        asp.setExpression(TRUE_EXPRESSION);
        asp.setResponseExpression(TRUE_EXPRESSION);
        asp.setCount(1);
        asp.setResponseCount(1);
        asp.setResponseSameThread(false);
        asp.start();
        asp.process(mockEvent);
        assertFalse(asp.expressionFailed());
        assertFalse(asp.responseExpressionFailed());
        assertFalse(asp.countFailOrNullEvent());
        assertFalse(asp.responseCountFailOrNullEvent());
    }

}
