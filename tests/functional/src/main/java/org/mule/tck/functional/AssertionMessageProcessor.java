/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.functional;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.AbstractMuleTestCase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;

public class AssertionMessageProcessor
    implements MessageProcessor, FlowConstructAware, Startable, MuleContextAware
{

    private String expression;
    private String message = "?";
    private int count = 1;

    public void setExpression(String expression)
    {
        this.expression = expression;
    }

    private int timeout = AbstractMuleTestCase.RECEIVE_TIMEOUT;

    private MuleEvent event;
    private CountDownLatch latch;

    private MuleContext muleContext;
    private FlowConstruct flowConstruct;
    private ExpressionManager expressionManager;
    private boolean result = true;

    public void start() throws InitialisationException
    {
        this.expressionManager = muleContext.getExpressionManager();
        this.expressionManager.validateExpression(expression);
        latch = new CountDownLatch(count);
        FlowAssert.addAssertion(flowConstruct.getName(), this);
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        this.event = event;
        result = result && expressionManager.evaluateBoolean(expression, event.getMessage(), false, true);
        latch.countDown();
        return event;
    }

    public void verify() throws InterruptedException
    {
        boolean didntTimeout = latch.await(timeout, TimeUnit.MILLISECONDS);
        if (!didntTimeout || event == null)
        {
            Assert.fail("Flow assertion '" + message + "' failed.  No message recieved.");
        }
        else if (!result)
        {
            Assert.fail("Flow assertion '" + message + "' failed. Expression " + expression
                        + " evaluated false.");
        }
    };

    public void reset()
    {
        this.event = null;

    }

    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public void setCount(int count)
    {
        this.count = count;
    }
}
