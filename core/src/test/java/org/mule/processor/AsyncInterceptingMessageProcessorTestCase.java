/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.processor;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.WorkManager;
import org.mule.api.context.WorkManagerSource;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.concurrent.Latch;

import java.beans.ExceptionListener;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class AsyncInterceptingMessageProcessorTestCase extends AbstractMuleTestCase
    implements ExceptionListener
{

    protected MessageProcessor messageProcessor;
    protected TestListener target = new TestListener();
    protected Exception exceptionThrown;
    protected Latch latch = new Latch();

    public AsyncInterceptingMessageProcessorTestCase()
    {
        setStartContext(true);
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        messageProcessor = createAsyncInterceptingMessageProcessor(target);
    }

    public void testProcessSync() throws Exception
    {
        MuleEvent event = getTestEvent(TEST_MESSAGE, getTestOutboundEndpoint("",
            "test://test?exchangePattern=request-response"));

        MuleEvent result = messageProcessor.process(event);

        assertSame(event, target.sensedEvent);
        assertSame(event, result);
    }

    public void testProcessAsync() throws Exception
    {
        MuleEvent event = getTestEvent(TEST_MESSAGE, getTestInboundEndpoint(MessageExchangePattern.ONE_WAY));

        MuleEvent result = messageProcessor.process(event);

        latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS);
        assertNotNull(target.sensedEvent);
        // Event is not the same because it gets copied in
        // AbstractMuleEventWork#run()
        assertNotSame(event, target.sensedEvent);
        assertEquals(event.getMessageAsString(), target.sensedEvent.getMessageAsString());

        assertNull(result);
        assertNull(exceptionThrown);
    }

    protected AsyncInterceptingMessageProcessor createAsyncInterceptingMessageProcessor(MessageProcessor listener)
        throws Exception
    {
        AsyncInterceptingMessageProcessor mp = new AsyncInterceptingMessageProcessor(
            new TestWorkManagerSource(), true,  this);
        mp.setListener(listener);
        return mp;
    }

    class TestListener implements MessageProcessor
    {
        MuleEvent sensedEvent;

        public MuleEvent process(MuleEvent event) throws MuleException
        {
            sensedEvent = event;
            latch.countDown();
            return event;
        }
    }

    public void exceptionThrown(Exception e)
    {
        exceptionThrown = e;
    }

    class TestWorkManagerSource implements WorkManagerSource
    {
        public WorkManager getWorkManager() throws MuleException
        {
            return muleContext.getWorkManager();
        }
    }

}
