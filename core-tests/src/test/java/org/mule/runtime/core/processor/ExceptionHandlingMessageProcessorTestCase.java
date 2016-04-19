/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.processor.InterceptingMessageProcessor;
import org.mule.runtime.core.processor.ExceptionHandlingMessageProcessor;

import org.junit.Ignore;
import org.junit.Test;

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
        mp.setListener(new ExceptionThrowingMessageProcessor());

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
        mp.setListener(new ExceptionThrowingMessageProcessor());

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
        mp.setListener(new ExceptionThrowingMessageProcessor());

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
        mp.setListener(new ExceptionThrowingMessageProcessor());

        MuleEvent event = createTestOutboundEvent(exceptionListener);

        MuleEvent resultEvent = mp.process(event);
        assertNotNull(resultEvent);
        assertNotNull("exception expected", resultEvent.getMessage().getExceptionPayload());
        assertTrue(resultEvent.getMessage().getExceptionPayload().getException() instanceof IllegalStateException);

        assertEquals(NullPayload.getInstance(), resultEvent.getMessage().getPayload());
        assertNotNull(exceptionListener.sensedException);
    }
}
