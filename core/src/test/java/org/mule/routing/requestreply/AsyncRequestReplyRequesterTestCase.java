/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.requestreply;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.WorkManager;
import org.mule.api.context.WorkManagerSource;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.RequestReplyRequesterMessageProcessor;
import org.mule.api.routing.ResponseTimeoutException;
import org.mule.api.service.Service;
import org.mule.api.source.MessageSource;
import org.mule.processor.AsyncInterceptingMessageProcessor;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.concurrent.Latch;

import java.beans.ExceptionListener;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.resource.spi.work.Work;

import org.junit.Test;

public class AsyncRequestReplyRequesterTestCase extends AbstractMuleContextTestCase
    implements ExceptionListener
{

    @Test
    public void testSingleEventNoTimeout() throws Exception
    {
        RequestReplyRequesterMessageProcessor asyncReplyMP = new TestAsyncRequestReplyRequester();
        SensingNullMessageProcessor target = getSensingNullMessageProcessor();

        asyncReplyMP.setListener(target);
        asyncReplyMP.setReplySource(target.getMessageSource());

        MuleEvent event = getTestEvent(TEST_MESSAGE, getTestService());

        MuleEvent resultEvent = asyncReplyMP.process(event);

        // Can't assert same because we copy event when we receive async reply
        assertEquals(event.getMessageAsString(), resultEvent.getMessageAsString());
        assertEquals(event.getMessage().getUniqueId(), resultEvent.getMessage().getUniqueId());
    }

    @Test
    public void testSingleEventNoTimeoutAsync() throws Exception
    {
        RequestReplyRequesterMessageProcessor asyncReplyMP = new TestAsyncRequestReplyRequester();
        SensingNullMessageProcessor target = getSensingNullMessageProcessor();
        AsyncInterceptingMessageProcessor asyncMP = new AsyncInterceptingMessageProcessor(
            new WorkManagerSource()
            {
                public WorkManager getWorkManager() throws MuleException
                {
                    return muleContext.getWorkManager();
                }
            }
        );

        asyncMP.setListener(target);
        asyncReplyMP.setListener(asyncMP);
        asyncReplyMP.setReplySource(target.getMessageSource());

        MuleEvent event = getTestEvent(TEST_MESSAGE, getTestService(),
            getTestInboundEndpoint(MessageExchangePattern.ONE_WAY));

        MuleEvent resultEvent = asyncReplyMP.process(event);

        // Can't assert same because we copy event for async and also on async reply currently
        assertEquals(event.getMessageAsString(), resultEvent.getMessageAsString());
        assertEquals(event.getMessage().getUniqueId(), resultEvent.getMessage().getUniqueId());
    }

    @Test
    public void testSingleEventTimeout() throws Exception
    {
        TestAsyncRequestReplyRequester asyncReplyMP = new TestAsyncRequestReplyRequester();
        asyncReplyMP.setTimeout(1);
        SensingNullMessageProcessor target = getSensingNullMessageProcessor();
        target.setWaitTime(50);
        AsyncInterceptingMessageProcessor asyncMP = new AsyncInterceptingMessageProcessor(
            new WorkManagerSource()
            {

                public WorkManager getWorkManager() throws MuleException
                {
                    return muleContext.getWorkManager();
                }
            }
        );

        asyncMP.setListener(target);
        asyncReplyMP.setListener(asyncMP);
        asyncReplyMP.setReplySource(target.getMessageSource());

        MuleEvent event = getTestEvent(TEST_MESSAGE, getTestService(),
            getTestInboundEndpoint(MessageExchangePattern.ONE_WAY));

        try
        {
            asyncReplyMP.process(event);
            fail("ResponseTimeoutException expected");
        }
        catch (Exception e)
        {
            assertEquals(ResponseTimeoutException.class, e.getClass());
        }
    }

    @Test
    public void returnsNullWhenInterruptedWhileWaitingForReply() throws Exception
    {
        final Latch fakeLatch = new Latch()
        {
            @Override
            public void await() throws InterruptedException
            {
                throw new InterruptedException();
            }
        };

        final RequestReplyRequesterMessageProcessor asyncReplyMP = new TestAsyncRequestReplyRequester()
        {
            @Override
            protected Latch createEventLock()
            {
                return fakeLatch;
            }
        };

        final MuleEvent event = getTestEvent(TEST_MESSAGE, getTestService(),
                                             getTestInboundEndpoint(MessageExchangePattern.ONE_WAY));

        final CountDownLatch processingLatch = new CountDownLatch(1);

        MessageProcessor target = mock(MessageProcessor.class);
        asyncReplyMP.setListener(target);

        MessageSource messageSource = mock(MessageSource.class);
        asyncReplyMP.setReplySource(messageSource);

        final boolean[] exceptionThrown = new boolean[1];
        final Object[] responseEvent = new Object[1];

        Thread thread = new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    responseEvent[0] = asyncReplyMP.process(event);
                }
                catch (MuleException e)
                {
                    exceptionThrown[0] = true;
                }
                finally
                {
                    processingLatch.countDown();
                }
            }
        });

        thread.start();
        assertTrue(processingLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        assertFalse(exceptionThrown[0]);
        assertNull(responseEvent[0]);
    }

    @Test
    public void testMultiple() throws Exception
    {
        final RequestReplyRequesterMessageProcessor asyncReplyMP = new TestAsyncRequestReplyRequester();
        SensingNullMessageProcessor target = getSensingNullMessageProcessor();
        target.setWaitTime(50);
        AsyncInterceptingMessageProcessor asyncMP = new AsyncInterceptingMessageProcessor(
            new WorkManagerSource()
            {

                public WorkManager getWorkManager() throws MuleException
                {
                    return muleContext.getWorkManager();
                }
            }
        );

        asyncMP.setListener(target);
        asyncReplyMP.setListener(asyncMP);
        asyncReplyMP.setReplySource(target.getMessageSource());

        final InboundEndpoint inboundEndpoint = getTestInboundEndpoint(MessageExchangePattern.ONE_WAY);
        final Service service = getTestService();

        for (int i = 0; i < 500; i++)
        {
            muleContext.getWorkManager().scheduleWork(new Work()
            {
                public void run()
                {
                    MuleEvent event;
                    try
                    {
                        event = getTestEvent(TEST_MESSAGE, service, inboundEndpoint);
                        MuleEvent resultEvent = asyncReplyMP.process(event);

                        // Can't assert same because we copy event for async currently
                        assertEquals(event.getMessageAsString(), resultEvent.getMessageAsString());
                        assertEquals(event.getMessage().getUniqueId(), resultEvent.getMessage().getUniqueId());
                    }
                    catch (Exception e)
                    {
                        throw new RuntimeException(e);
                    }
                }

                public void release()
                {
                    // nop
                }
            });
        }
    }

    public void exceptionThrown(Exception e)
    {
        e.printStackTrace();
        fail(e.getMessage());
    }

    class TestAsyncRequestReplyRequester extends AbstractAsyncRequestReplyRequester
    {
        // no custom methods
    }
}
