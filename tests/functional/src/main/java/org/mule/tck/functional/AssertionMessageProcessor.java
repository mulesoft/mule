/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.functional;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.concurrent.Latch;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;

public class AssertionMessageProcessor implements MessageProcessor, FlowConstructAware, Startable
{

    private String expression;

    public void setExpression(String expression)
    {
        this.expression = expression;
    }

    private int timeout = AbstractMuleTestCase.RECEIVE_TIMEOUT;

    private MuleEvent event;
    private Latch latch = new Latch();

    private FlowConstruct flowConstruct;
    private ExpressionManager expressionManager;

    @Override
    public void start() throws InitialisationException
    {
        this.expressionManager = flowConstruct.getMuleContext().getExpressionManager();
        this.expressionManager.validateExpression(expression);
        FlowAssert.addAssertion(flowConstruct.getName(), this);
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        this.event = event;
        latch.countDown();
        return event;
    }

    public void verify() throws InterruptedException
    {
        latch.await(timeout, TimeUnit.MILLISECONDS);
        if (event == null)
        {
            Assert.fail("event is null");
        }
        else if (!expressionManager.evaluateBoolean(expression, event.getMessage(), false, true))
        {
            Assert.fail("Flow assertion failed: " + expression);
        }
    };

    public void reset()
    {
        this.event = null;

    }

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }

}
