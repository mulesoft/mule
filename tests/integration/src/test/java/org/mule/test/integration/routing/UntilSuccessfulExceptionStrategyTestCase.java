/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.message.DefaultExceptionPayload;
import org.mule.util.concurrent.Latch;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

public class UntilSuccessfulExceptionStrategyTestCase extends FunctionalTestCase
{

    private static final int TIMEOUT = 10;
    private static Latch latch;

    @Before
    public void setUp()
    {
        latch = new Latch();
    }

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/routing/until-successful-exception-strategy-config.xml";
    }

    @Test
    public void usingSimpleSetUp() throws Exception
    {
        testHandlingOfFailures("simpleTest");
    }

    @Test
    public void usingSimpleSetUpAndNoRetries() throws Exception
    {
        testHandlingOfFailures("noRetriesSimpleTest");
    }

    @Test
    public void usingSplitterAggregator() throws Exception
    {
        testHandlingOfFailures("withSplitterAggregatorTest");
    }

    @Test
    public void usingSplitterAggregatorAndNoRetries() throws Exception
    {
        testHandlingOfFailures("noRetriesSplitterAggregatorTest");
    }

    private void testHandlingOfFailures(String entryPoint) throws Exception
    {
        MuleMessage response = flowRunner(entryPoint).withPayload(getTestMuleMessage()).run().getMessage();
        assertThat(response.getExceptionPayload(), is(nullValue()));
        assertThat(getPayloadAsString(response), is("ok"));
    }

    public static class LockProcessor implements MessageProcessor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            try
            {
                if (!latch.await(TIMEOUT, TimeUnit.SECONDS))
                {
                    event.getMessage().setExceptionPayload(new DefaultExceptionPayload(new RuntimeException()));
                }
            }
            catch (InterruptedException e)
            {
                //do nothing
            }
            return event;
        }
    }

    public static class UnlockProcessor implements MessageProcessor
    {
        AtomicInteger count;

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            if (count.decrementAndGet() == 0)
            {
                latch.release();
            }
            return event;
        }

    }

    public static class WaitTwiceBeforeUnlockProcessor extends UnlockProcessor
    {
        public WaitTwiceBeforeUnlockProcessor()
        {
            count = new AtomicInteger(2);
        }

    }

    public static class WaitOnceBeforeUnlockProcessor extends UnlockProcessor
    {
        public WaitOnceBeforeUnlockProcessor()
        {
            count = new AtomicInteger(1);
        }
    }
}
