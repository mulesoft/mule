/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.processor;

import org.junit.Ignore;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.endpoint.AbstractMessageProcessorTestCase;
import org.mule.transport.NullPayload;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@Ignore
//Test is ignored since ExceptionHandlingMessageProcessor is not longer used - should be removed in next major release
public class ExceptionHandlingMessageProcessorTestCase extends AbstractMessageProcessorTestCase
{
    private TestExceptionListener exceptionListener;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        exceptionListener = new TestExceptionListener();
    }

    @Test
    public void testNoCatch() throws Exception
    {
        OutboundEndpoint endpoint = createTestOutboundEndpoint(null, null);
        InterceptingMessageProcessor mp = new ExceptionHandlingMessageProcessor();
        TestListener listener = new TestListener();
        mp.setListener(listener);

        MuleEvent event = createTestOutboundEvent();

        MuleEvent result = mp.process(event);

        assertSame(event, listener.sensedEvent);
        assertSame(event, result);
        assertNull(exceptionListener.sensedException);
    }

    @Test
    public void testCatchRuntimeExceptionSync() throws Exception
    {
        OutboundEndpoint endpoint = createTestOutboundEndpoint(null, null);
        InterceptingMessageProcessor mp = new ExceptionHandlingMessageProcessor();
        mp.setListener(new ExceptionThrowingMessageProcessr());

        MuleEvent event = createTestOutboundEvent(exceptionListener);

        MuleEvent resultEvent = mp.process(event);
        assertNotNull(resultEvent);
        assertNotNull("exception expected", resultEvent.getMessage().getExceptionPayload());
        assertTrue(resultEvent.getMessage().getExceptionPayload().getException() instanceof IllegalStateException);

        assertEquals(NullPayload.getInstance(), resultEvent.getMessage().getPayload());
        assertNotNull(exceptionListener.sensedException);
    }

    @Test
    public void testCatchRuntimeExceptionAsync() throws Exception
    {
        OutboundEndpoint endpoint = createTestOutboundEndpoint(null, null, 
            MessageExchangePattern.ONE_WAY, null);
        InterceptingMessageProcessor mp = new ExceptionHandlingMessageProcessor();
        mp.setListener(new ExceptionThrowingMessageProcessr());

        MuleEvent event = createTestOutboundEvent(exceptionListener);

        MuleEvent resultEvent = mp.process(event);
        assertNotNull(resultEvent);
        assertNotNull("exception expected", resultEvent.getMessage().getExceptionPayload());
        assertTrue(resultEvent.getMessage().getExceptionPayload().getException() instanceof IllegalStateException);

        assertEquals(NullPayload.getInstance(), resultEvent.getMessage().getPayload());
        assertNotNull(exceptionListener.sensedException);
    }

    @Test
    public void testCatchDispatchExceptionSync() throws Exception
    {
        OutboundEndpoint endpoint = createTestOutboundEndpoint(null, null);
        InterceptingMessageProcessor mp = new ExceptionHandlingMessageProcessor();
        mp.setListener(new ExceptionThrowingMessageProcessr());

        MuleEvent event = createTestOutboundEvent(exceptionListener);

        MuleEvent resultEvent = mp.process(event);
        assertNotNull(resultEvent);
        assertNotNull("exception expected", resultEvent.getMessage().getExceptionPayload());
        assertTrue(resultEvent.getMessage().getExceptionPayload().getException() instanceof IllegalStateException);

        assertEquals(NullPayload.getInstance(), resultEvent.getMessage().getPayload());
        assertNotNull(exceptionListener.sensedException);
    }

    @Test
    public void testCatchDispatchExceptionAsync() throws Exception
    {
        OutboundEndpoint endpoint = createTestOutboundEndpoint(null, null, 
            MessageExchangePattern.ONE_WAY, null);
        InterceptingMessageProcessor mp = new ExceptionHandlingMessageProcessor();
        mp.setListener(new ExceptionThrowingMessageProcessr());

        MuleEvent event = createTestOutboundEvent(exceptionListener);

        MuleEvent resultEvent = mp.process(event);
        assertNotNull(resultEvent);
        assertNotNull("exception expected", resultEvent.getMessage().getExceptionPayload());
        assertTrue(resultEvent.getMessage().getExceptionPayload().getException() instanceof IllegalStateException);

        assertEquals(NullPayload.getInstance(), resultEvent.getMessage().getPayload());
        assertNotNull(exceptionListener.sensedException);
    }
}
