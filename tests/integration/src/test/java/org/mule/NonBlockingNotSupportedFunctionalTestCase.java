/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.functional.functional.EventCallback;
import org.mule.functional.functional.FlowAssert;
import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.functional.junit4.FlowRunner;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.util.concurrent.Latch;

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
        flowRunner("syncFlow").withPayload(TEST_MESSAGE).nonBlocking().run();
    }

    @Test
    public void catchExceptionStrategy() throws Exception
    {
        flowRunner("catchExceptionStrategy").withPayload(TEST_MESSAGE).nonBlocking().run();
    }

    @Test
    public void splitter() throws Exception
    {
        flowRunner("splitter").withPayload(new String[] {"1", "2", "3"}).nonBlocking().run();
    }

    @Test
    public void untilSuccessful() throws Exception
    {
        flowRunner("untilSuccessful").withPayload(TEST_MESSAGE).nonBlocking().run();
    }

    @Test
    public void scatterGather() throws Exception
    {
        flowRunner("scatterGather").withPayload(TEST_MESSAGE).nonBlocking().run();
    }

    @Test
    public void all() throws Exception
    {
        flowRunner("all").withPayload(TEST_MESSAGE).nonBlocking().run();
    }

    @Test
    public void firstSuccessful() throws Exception
    {
        flowRunner("firstSuccessful").withPayload(TEST_MESSAGE).nonBlocking().run();
    }

    @Test
    public void roundRobin() throws Exception
    {
        flowRunner("roundRobin").withPayload(TEST_MESSAGE).nonBlocking().run();
    }

    @Test
    public void requestReply() throws Exception
    {
        flowRunner("requestReply").withPayload(TEST_MESSAGE).nonBlocking().run();
    }

    @Test
    public void aggregator() throws Exception
    {
        String correlationId = "id";
        int correlationGroupSize = 3;

        FlowRunner runner = flowRunner("aggregator").withPayload(TEST_MESSAGE).nonBlocking();
        MuleMessage message1 = runner.buildEvent().getMessage();
        message1.setCorrelationId(correlationId);
        message1.setCorrelationGroupSize(correlationGroupSize);
        message1.setCorrelationSequence(1);
        runner.runNoVerify();

        runner.reset();
        MuleMessage message2 = runner.buildEvent().getMessage();
        message2.setCorrelationId(correlationId);
        message2.setCorrelationGroupSize(correlationGroupSize);
        message2.setCorrelationSequence(2);
        message2.setMessageRootId(message1.getMessageRootId());
        runner.runNoVerify();

        runner.reset();
        MuleMessage message3 = runner.buildEvent().getMessage();
        message3.setCorrelationId(correlationId);
        message3.setCorrelationGroupSize(correlationGroupSize);
        message3.setCorrelationSequence(3);
        message3.setMessageRootId(message1.getMessageRootId());
        runner.run();
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
        flowRunner("transactional").withPayload(TEST_MESSAGE).nonBlocking().run();
    }

}

