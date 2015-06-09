/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.construct.Flow;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FlowAssert;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.concurrent.Latch;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class NonBlockingNotSupportedFunctionalTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "non-blocking-not-supported-test-config.xml";
    }

    @Test
    public void syncFlow() throws Exception
    {
        testFlowNonBlocking("syncFlow");
    }

    @Test
    public void catchExceptionStrategy() throws Exception
    {
        testFlowNonBlocking("catchExceptionStrategy");
    }

    @Test
    public void splitter() throws Exception
    {
        testFlowNonBlocking("splitter", getTestEventUsingFlow(new String[] {"1", "2", "3"}));
    }

    @Test
    public void untilSuccessful() throws Exception
    {
        testFlowNonBlocking("untilSuccessful");
    }

    @Test
    public void scatterGather() throws Exception
    {
        testFlowNonBlocking("scatterGather");
    }

    @Test
    public void all() throws Exception
    {
        testFlowNonBlocking("all");
    }

    @Test
    public void firstSuccessful() throws Exception
    {
        testFlowNonBlocking("firstSuccessful");
    }

    @Test
    public void roundRobin() throws Exception
    {
        testFlowNonBlocking("roundRobin");
    }

    @Test
    public void requestReply() throws Exception
    {
        testFlowNonBlocking("requestReply");
    }

    @Test
    public void aggregator() throws Exception
    {
        Flow flow = lookupFlowConstruct("aggregator");
        String correlationId = "id";
        int correlationGroupSize = 3;

        MuleMessage message1 = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        message1.setCorrelationId(correlationId);
        message1.setCorrelationGroupSize(correlationGroupSize);
        message1.setCorrelationSequence(1);
        runFlowNonBlocking("aggregator", new DefaultMuleEvent(message1, MessageExchangePattern.REQUEST_RESPONSE, flow));

        MuleMessage message2 = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        message2.setCorrelationId(correlationId);
        message2.setCorrelationGroupSize(correlationGroupSize);
        message2.setCorrelationSequence(2);
        message2.setMessageRootId(message1.getMessageRootId());
        runFlowNonBlocking("aggregator", new DefaultMuleEvent(message2, MessageExchangePattern.REQUEST_RESPONSE, flow));

        MuleMessage message3 = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        message3.setCorrelationId(correlationId);
        message3.setCorrelationGroupSize(correlationGroupSize);
        message3.setCorrelationSequence(3);
        message3.setMessageRootId(message1.getMessageRootId());
        runFlowNonBlocking("aggregator", new DefaultMuleEvent(message3, MessageExchangePattern.REQUEST_RESPONSE, flow));

        FlowAssert.verify("aggregator");
    }

    @Test
    public void poll() throws Exception
    {
        final Latch latch = new Latch();
        ((FunctionalTestComponent) getComponent("poll")).setEventCallback(new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                latch.countDown();
            }
        });
        latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS);
        FlowAssert.verify("poll");
    }

    @Test
    public void transactional() throws Exception
    {
        testFlowNonBlocking("transactional");
    }

}

