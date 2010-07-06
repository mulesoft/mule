/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.endpoint.outbound;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.filter.Filter;
import org.mule.api.security.EndpointSecurityFilter;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.DispatchException;
import org.mule.config.i18n.CoreMessages;

import java.beans.ExceptionListener;

public class OutboundTryCatchMessageProcessorTestCase extends AbstractOutboundMessageProcessorTestCase
{

    private TestExceptionListener exceptionListener;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        exceptionListener = new TestExceptionListener();
    }

    public void testNoCatch() throws Exception
    {
        OutboundEndpoint endpoint = createTestOutboundEndpoint(null, null);
        InterceptingMessageProcessor mp = new OutboundTryCatchMessageProcessor(endpoint);
        TestListener listener = new TestListener();
        mp.setListener(listener);

        MuleEvent event = createTestOutboundEvent(endpoint);

        MuleEvent result = mp.process(event);

        assertSame(event, listener.sensedEvent);
        assertSame(event, result);
        assertNull(exceptionListener.sensedException);
    }

    public void testCatchRuntimeExceptionSync() throws Exception
    {
        OutboundEndpoint endpoint = createTestOutboundEndpoint(null, null);
        InterceptingMessageProcessor mp = new OutboundTryCatchMessageProcessor(endpoint);
        mp.setListener(new ExceptionThrowingMessageProcessr());

        MuleEvent event = createTestOutboundEvent(endpoint);

        MuleEvent result = null;
        try
        {
            result = mp.process(event);
            fail("Exception expected");
        }
        catch (Exception e)
        {
            assertEquals(DispatchException.class, e.getClass());
        }

        assertNull(result);
        assertNotNull(exceptionListener.sensedException);
    }

    public void testCatchRuntimeExceptionAsync() throws Exception
    {

        OutboundEndpoint endpoint = createTestOutboundEndpoint(null, null, false, null);
        InterceptingMessageProcessor mp = new OutboundTryCatchMessageProcessor(endpoint);
        mp.setListener(new ExceptionThrowingMessageProcessr());

        MuleEvent event = createTestOutboundEvent(endpoint);

        MuleEvent result = null;
        try
        {
            result = mp.process(event);
        }
        catch (Exception e)
        {
            fail("No exception expected");
        }

        assertNull(result);
        assertNotNull(exceptionListener.sensedException);
    }

    public void testCatchDispatchExceptionSync() throws Exception
    {
        OutboundEndpoint endpoint = createTestOutboundEndpoint(null, null);
        InterceptingMessageProcessor mp = new OutboundTryCatchMessageProcessor(endpoint);
        mp.setListener(new ExceptionThrowingMessageProcessr());

        MuleEvent event = createTestOutboundEvent(endpoint);

        MuleEvent result = null;
        try
        {
            result = mp.process(event);
            fail("Exception expected");
        }
        catch (Exception e)
        {
            assertEquals(DispatchException.class, e.getClass());
        }

        assertNull(result);
        assertNotNull(exceptionListener.sensedException);
    }

    public void testCatchDispatchExceptionAsync() throws Exception
    {
        OutboundEndpoint endpoint = createTestOutboundEndpoint(null, null, false, null);
        InterceptingMessageProcessor mp = new OutboundTryCatchMessageProcessor(endpoint);
        mp.setListener(new ExceptionThrowingMessageProcessr());

        MuleEvent event = createTestOutboundEvent(endpoint);

        MuleEvent result = null;
        try
        {
            result = mp.process(event);
        }
        catch (Exception e)
        {
            fail("No exception expected");
        }

        assertNull(result);
        assertNotNull(exceptionListener.sensedException);
    }

    protected OutboundEndpoint createTestOutboundEndpoint(Filter filter,
                                                          EndpointSecurityFilter securityFilter,
                                                          Transformer transformer,
                                                          Transformer responseTransformer,
                                                          boolean sync,
                                                          TransactionConfig txConfig)
        throws EndpointException, InitialisationException
    {
        OutboundEndpoint endpoint = super.createTestOutboundEndpoint(filter, securityFilter, transformer,
            responseTransformer, sync, txConfig);
        endpoint.getConnector().setExceptionListener(exceptionListener);
        return endpoint;
    }

    static class ExceptionThrowingMessageProcessr implements MessageProcessor
    {
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            throw new IllegalStateException();
        }
    };

    static class DispatchExceptionThrowingMessageProcessr implements MessageProcessor
    {
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            throw new DispatchException(CoreMessages.createStaticMessage("exception"), event.getMessage(),
                event.getEndpoint());
        }
    };

    static class TestExceptionListener implements ExceptionListener
    {
        Exception sensedException;

        public void exceptionThrown(Exception e)
        {
            sensedException = e;
        }
    }

}
