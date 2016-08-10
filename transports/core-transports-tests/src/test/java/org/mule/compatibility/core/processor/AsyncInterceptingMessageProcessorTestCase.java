/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.processor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.context.WorkManager;
import org.mule.runtime.core.api.context.WorkManagerSource;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.processor.AsyncInterceptingMessageProcessor;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.tck.junit4.AbstractMuleContextEndpointTestCase;
import org.mule.tck.testmodels.mule.TestTransaction;

import java.beans.ExceptionListener;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class AsyncInterceptingMessageProcessorTestCase extends AbstractMuleContextEndpointTestCase
    implements ExceptionListener
{

    public static final String EXPECTING_SYNCHRONOUS_EVENT_ERROR = "Exception expected: '" + AsyncInterceptingMessageProcessor.SYNCHRONOUS_NONBLOCKING_EVENT_ERROR_MESSAGE + "'";

    protected AsyncInterceptingMessageProcessor messageProcessor;
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

    @Test
    public void testProcessOneWay() throws Exception
    {
        MuleEvent event = getTestEvent(TEST_MESSAGE, getTestInboundEndpoint(MessageExchangePattern.ONE_WAY));

        assertAsync(messageProcessor, event);
    }

    @Test
    public void testProcessRequestResponse() throws Exception
    {
        MuleEvent event = getTestEvent(TEST_MESSAGE, getTestInboundEndpoint(MessageExchangePattern.REQUEST_RESPONSE));

        try
        {
            messageProcessor.process(event);
            fail(EXPECTING_SYNCHRONOUS_EVENT_ERROR);
        }
        catch (Exception e)
        {
        }
    }

    @Test
    public void testProcessOneWayWithTx() throws Exception
    {
        MuleEvent event = getTestEvent(TEST_MESSAGE, getTestTransactedInboundEndpoint(MessageExchangePattern.ONE_WAY));
        Transaction transaction = new TestTransaction(muleContext);
        TransactionCoordination.getInstance().bindTransaction(transaction);

        try
        {
            messageProcessor.process(event);
            fail(EXPECTING_SYNCHRONOUS_EVENT_ERROR);
        }
        catch (Exception e)
        {
        }
        finally
        {
            TransactionCoordination.getInstance().unbindTransaction(transaction);
        }
    }

    @Test
    public void testProcessRequestResponseWithTx() throws Exception
    {
        MuleEvent event = getTestEvent(TEST_MESSAGE, getTestTransactedInboundEndpoint(MessageExchangePattern.REQUEST_RESPONSE));
        Transaction transaction = new TestTransaction(muleContext);
        TransactionCoordination.getInstance().bindTransaction(transaction);

        try
        {
            messageProcessor.process(event);
            fail(EXPECTING_SYNCHRONOUS_EVENT_ERROR);
        }
        catch (Exception e)
        {
        }
        finally
        {
            TransactionCoordination.getInstance().unbindTransaction(transaction);
        }
    }

    protected void assertAsync(MessageProcessor processor, MuleEvent event)
            throws MuleException, InterruptedException
    {
        MuleEvent result = processor.process(event);

        latch.await(10000, TimeUnit.MILLISECONDS);
        assertNotNull(target.sensedEvent);
        // Event is not the same because it gets copied in
        // AbstractMuleEventWork#run()
        assertNotSame(event, target.sensedEvent);
        assertEquals(event.getMessageAsString(), target.sensedEvent.getMessageAsString());
        assertNotSame(Thread.currentThread(), target.thread);

        assertSame(VoidMuleEvent.getInstance(), result);
        assertNull(exceptionThrown);
    }

    protected AsyncInterceptingMessageProcessor createAsyncInterceptingMessageProcessor(MessageProcessor listener)
            throws Exception
    {
        AsyncInterceptingMessageProcessor mp = new AsyncInterceptingMessageProcessor(
                new TestWorkManagerSource());
        mp.setMuleContext(muleContext);
        mp.setListener(listener);
        return mp;
    }

    class TestListener implements MessageProcessor
    {

        MuleEvent sensedEvent;
        Thread thread;

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            sensedEvent = event;
            thread = Thread.currentThread();
            latch.countDown();
            return event;
        }
    }

    @Override
    public void exceptionThrown(Exception e)
    {
        exceptionThrown = e;
    }

    class TestWorkManagerSource implements WorkManagerSource
    {
        @Override
        public WorkManager getWorkManager() throws MuleException
        {
            return muleContext.getWorkManager();
        }
    }

}
