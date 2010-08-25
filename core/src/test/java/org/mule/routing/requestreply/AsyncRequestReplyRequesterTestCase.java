/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.requestreply;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.WorkManager;
import org.mule.api.context.WorkManagerSource;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.RequestReplyRequesterMessageProcessor;
import org.mule.api.routing.ResponseTimeoutException;
import org.mule.api.service.Service;
import org.mule.processor.AsyncInterceptingMessageProcessor;
import org.mule.routing.requestreply.AbstractAsyncRequestReplyRequester;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.SensingNullMessageProcessor;

import java.beans.ExceptionListener;

import javax.resource.spi.work.Work;

public class AsyncRequestReplyRequesterTestCase extends AbstractMuleTestCase
    implements ExceptionListener
{

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
            }, true);

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
            }, true);

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
            }, true);

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

    }
}
