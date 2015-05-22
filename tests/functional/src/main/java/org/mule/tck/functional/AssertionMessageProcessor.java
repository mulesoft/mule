/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;

public class AssertionMessageProcessor implements MessageProcessor, FlowConstructAware, Startable
{
    protected String expression  = "#[true]";
    protected String message = "?";
    private int count = 1;
    private int invocationCount = 0;
    protected boolean needToMatchCount = false;

    public void setExpression(String expression)
    {
        this.expression = expression;
    }

    protected int timeout = AbstractMuleTestCase.RECEIVE_TIMEOUT;

    private MuleEvent event;
    private CountDownLatch latch;

    protected FlowConstruct flowConstruct;
    protected ExpressionManager expressionManager;
    private boolean result = true;

    @Override
    public void start() throws InitialisationException
    {
        this.expressionManager = flowConstruct.getMuleContext().getExpressionManager();
        this.expressionManager.validateExpression(expression);
        latch = new CountDownLatch(count);
        FlowAssert.addAssertion(flowConstruct.getName(), this);
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (event == null)
        {
            return null;
        }
        this.event = event;
        result = result && expressionManager.evaluateBoolean(expression, event, false, true);
        increaseCount();
        latch.countDown();
        return event;
    }

    /**
     * If result evaluated to false in some processed event or the last processed
     * event was null, then assert fails, otherwise:
     * <li>count was set & count processes were done => ok</li>
     * <li>count was set & count processes were not done => fail</li>
     * <li>count was not set & at least one processing were done => ok</li>
     * @throws InterruptedException
     */
    public void verify() throws InterruptedException
    {
        if (countFailOrNullEvent())
        {
            Assert.fail("Flow assertion '" + message + "' failed. No message received or if count attribute was " +
                    "set then it was no matched.");
        }
        else if (expressionFailed())
        {
            Assert.fail("Flow assertion '" + message + "' failed. Expression " + expression
                        + " evaluated false.");
        }
    }
    
    public Boolean countFailOrNullEvent() throws InterruptedException  //added for testing (cant assert on asserts)
    {
        return !isProcessesCountCorrect();
    }

    public Boolean expressionFailed()  //added for testing (cant assert on asserts)
    {
        return !result;
    }

    public void reset()
    {
        this.event = null;
    }

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public void setCount(int count)
    {
        this.count = count;
        needToMatchCount = true;
    }

    private void increaseCount()
    {
        invocationCount++;
    }

    /**
     * The semantics of the count are as follows:
     * - count was set & count processes were done => ok
     * - count was set & count processes were not done => fail
     * - count was not set & at least one processing were done => ok
     * @return
     * @throws InterruptedException
     */
    synchronized private boolean isProcessesCountCorrect() throws InterruptedException
    {
        boolean countReached = latch.await(timeout, TimeUnit.MILLISECONDS);
        if (needToMatchCount)
        {
            return count == invocationCount;
        }
        else
        {
            return countReached;
        }
    }
}
