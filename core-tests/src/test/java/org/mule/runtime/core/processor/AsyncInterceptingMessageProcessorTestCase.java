/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.ThreadSafeAccess;
import org.mule.runtime.core.api.context.WorkManager;
import org.mule.runtime.core.api.context.WorkManagerSource;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.exception.RollbackSourceCallback;
import org.mule.runtime.core.api.exception.SystemExceptionHandler;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.routing.filters.WildcardFilter;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.mule.TestTransaction;

import java.beans.ExceptionListener;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class AsyncInterceptingMessageProcessorTestCase extends AbstractMuleContextTestCase
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

    @Test
    public void testWorkMessagingException() throws Exception
    {

        Flow flow = new Flow("flow", muleContext);
        LatchedExceptionListener exceptionListener = new LatchedExceptionListener();
        flow.setExceptionListener(exceptionListener);
        initialiseObject(flow);

        MuleEvent event = getTestEvent(TEST_MESSAGE, flow, MessageExchangePattern.ONE_WAY);

        MessageProcessor next = new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                throw new MessagingException(event, null);
            }
        };

        messageProcessor.setListener(next);

        messageProcessor.process(event);

        assertTrue(exceptionListener.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testWorkException() throws Exception
    {

        Flow flow = new Flow("flow", muleContext);
        LatchedExceptionListener exceptionListener = new LatchedExceptionListener();
        flow.setExceptionListener(exceptionListener);
        initialiseObject(flow);

        MuleEvent event = getTestEvent(TEST_MESSAGE, flow, MessageExchangePattern.ONE_WAY);

        MessageProcessor next = new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                throw new DefaultMuleException("failure");
            }
        };

        messageProcessor.setListener(next);
        messageProcessor.setMuleContext(muleContext);
        messageProcessor.process(event);

        assertTrue(exceptionListener.latch .await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    protected void assertSync(MessageProcessor processor, MuleEvent event) throws MuleException
    {
        MuleEvent result = processor.process(event);

        assertSame(event, target.sensedEvent);
        assertSame(event, result);
        assertSame(Thread.currentThread(), target.thread);
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
            thread = Thread.currentThread();
            sensedEvent = event;
            latch.countDown();
            ((DefaultMuleMessage) sensedEvent.getMessage()).assertAccess(ThreadSafeAccess.WRITE);
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

    private static class LatchedExceptionListener implements MessagingExceptionHandler
    {

        Latch latch = new Latch();

        public WildcardFilter getCommitTxFilter()
        {
            return null;
        }

        public WildcardFilter getRollbackTxFilter()
        {
            return null;
        }

        @Override
        public MuleEvent handleException(Exception exception, MuleEvent event)
        {
            latch.countDown();
            return null;
        }

    }

    private static class LatchedSystemExceptionHandler implements SystemExceptionHandler
    {

        Latch latch = new Latch();

        @Override
        public void handleException(Exception exception, RollbackSourceCallback rollbackMethod)
        {
            latch.countDown();
        }

        @Override
        public void handleException(Exception exception)
        {
            latch.countDown();
        }
    }

}
