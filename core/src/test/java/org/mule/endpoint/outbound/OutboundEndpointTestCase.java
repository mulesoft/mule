/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.endpoint.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.MessageExchangePattern;
import org.mule.RequestContext;
import org.mule.VoidMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.routing.filter.Filter;
import org.mule.api.security.SecurityFilter;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.Connector;
import org.mule.api.transport.MessageDispatcher;
import org.mule.context.notification.EndpointMessageNotification;
import org.mule.context.notification.SecurityNotification;
import org.mule.endpoint.AbstractMessageProcessorTestCase;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.tck.security.TestSecurityFilter;
import org.mule.tck.testmodels.mule.TestMessageDispatcher;
import org.mule.tck.testmodels.mule.TestMessageDispatcherFactory;
import org.mule.transformer.simple.OutboundAppendTransformer;
import org.mule.transformer.simple.ResponseAppendTransformer;
import org.mule.transport.AbstractMessageDispatcher;
import org.mule.util.concurrent.Latch;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.Mockito;

/**
 * Tests flow of messages from {@link OutboundEndpoint#process(MuleEvent)} down to
 * {@link AbstractMessageDispatcher} and the chain of MessageProcessor's that
 * implement the outbound endpoint processing.
 */
public class OutboundEndpointTestCase extends AbstractMessageProcessorTestCase
{
    protected FakeMessageDispatcher dispacher;
    protected MuleEvent testOutboundEvent;

    @Test
    public void testDefaultFlowSync() throws Exception
    {
        Transformer reqTransformer = Mockito.mock(Transformer.class);
        Mockito.when(reqTransformer.process(Mockito.any(MuleEvent.class))).then(echoEventAnswer);
        Transformer resTransformer = Mockito.mock(Transformer.class);
        Mockito.when(resTransformer.process(Mockito.any(MuleEvent.class))).then(echoEventAnswer);
        
        OutboundEndpoint endpoint = createOutboundEndpoint(null, null, reqTransformer, resTransformer, 
            MessageExchangePattern.REQUEST_RESPONSE, null);

        testOutboundEvent = createTestOutboundEvent();
        MuleEvent result = endpoint.process(testOutboundEvent);

        Mockito.verify(reqTransformer, Mockito.times(1)).process(Mockito.any(MuleEvent.class));
        Mockito.verify(resTransformer, Mockito.times(1)).process(Mockito.any(MuleEvent.class));
        
        assertMessageSentSame(true);

        assertSame(responseMessage, result.getMessage());

        assertEqualMessages(responseMessage, result.getMessage());
    }

    @Test
    public void testDefaultFlowAsync() throws Exception
    {
        Transformer reqTransformer = Mockito.mock(Transformer.class);
        Mockito.when(reqTransformer.process(Mockito.any(MuleEvent.class))).then(echoEventAnswer);
        Transformer resTransformer = Mockito.mock(Transformer.class);
        Mockito.when(resTransformer.process(Mockito.any(MuleEvent.class))).then(echoEventAnswer);

        OutboundEndpoint endpoint = createOutboundEndpoint(null, null, reqTransformer, resTransformer, 
            MessageExchangePattern.ONE_WAY, null);

        testOutboundEvent = createTestOutboundEvent();
        MuleEvent result = endpoint.process(testOutboundEvent);
        
        Mockito.verify(reqTransformer, Mockito.times(1)).process(Mockito.any(MuleEvent.class));
        Mockito.verify(resTransformer, Mockito.never()).process(Mockito.any(MuleEvent.class));

        dispacher.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS);
        assertMessageSentSame(false);
        assertSame(VoidMuleEvent.getInstance(), result);
    }

    @Test
    public void testSecurityFilterAccept() throws Exception
    {
        OutboundEndpoint endpoint = createOutboundEndpoint(null, new TestSecurityFilter(true), 
            null, null, MessageExchangePattern.REQUEST_RESPONSE, null);

        testOutboundEvent = createTestOutboundEvent();
        MuleEvent result = endpoint.process(testOutboundEvent);

        assertMessageSentSame(true);

        assertSame(responseMessage, result.getMessage());

        assertEqualMessages(responseMessage, result.getMessage());
    }

    @Test
    public void testSecurityFilterNotAccept() throws Exception
    {
        TestSecurityNotificationListener securityNotificationListener = new TestSecurityNotificationListener();
        muleContext.registerListener(securityNotificationListener);

        OutboundEndpoint endpoint = createOutboundEndpoint(null, new TestSecurityFilter(false), 
            null, null, MessageExchangePattern.REQUEST_RESPONSE, null);

        testOutboundEvent = createTestOutboundEvent();
        RequestContext.setEvent(testOutboundEvent);
        try
        {
            endpoint.process(testOutboundEvent);
            fail("Exception expected");
        }
        catch (TestSecurityFilter.StaticMessageUnauthorisedException e)
        {
            testOutboundEvent.getFlowConstruct().getExceptionListener().handleException(e, testOutboundEvent);
        }

        assertMessageNotSent();

        assertTrue(securityNotificationListener.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        assertEquals(SecurityNotification.SECURITY_AUTHENTICATION_FAILED,
            securityNotificationListener.securityNotification.getAction());
        assertEquals(securityNotificationListener.securityNotification.getResourceIdentifier(),
            TestSecurityFilter.StaticMessageUnauthorisedException.class.getName());
    }

    @Test
    public void testSendNotfication() throws Exception
    {
        TestEndpointMessageNotificationListener listener = new TestEndpointMessageNotificationListener(2);
        muleContext.registerListener(listener);

        OutboundEndpoint endpoint = createOutboundEndpoint(null, null, null, null, 
            MessageExchangePattern.REQUEST_RESPONSE, null);
        MuleEvent outboundEvent = createTestOutboundEvent();
        endpoint.process(outboundEvent);

        assertTrue(listener.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        assertEquals(2, listener.messageNotificationList.size());
        assertEquals(EndpointMessageNotification.MESSAGE_SEND_BEGIN, listener.messageNotificationList.get(0).getAction());
        assertEquals(EndpointMessageNotification.MESSAGE_SEND_END, listener.messageNotificationList.get(1).getAction());
        assertEquals(endpoint.getEndpointURI().getUri().toString(),
            listener.messageNotificationList.get(0).getEndpoint());
                assertEquals(endpoint.getEndpointURI().getUri().toString(),
            listener.messageNotificationList.get(1).getEndpoint());
        assertTrue(listener.messageNotificationList.get(0).getSource() instanceof MuleMessage);
        assertTrue(listener.messageNotificationList.get(1).getSource() instanceof MuleMessage);
        assertEquals(outboundEvent.getMessage().getPayload(),
            listener.messageNotificationList.get(0).getSource().getPayload());
        assertEquals(RESPONSE_MESSAGE,
            listener.messageNotificationList.get(1).getSource().getPayload());
    }

    @Test
    public void testDispatchNotfication() throws Exception
    {
        TestEndpointMessageNotificationListener listener = new TestEndpointMessageNotificationListener(2);
        muleContext.registerListener(listener);

        OutboundEndpoint endpoint = createOutboundEndpoint(null, null, null, null, 
            MessageExchangePattern.ONE_WAY, null);
        MuleEvent outboundEvent = createTestOutboundEvent();
        endpoint.process(outboundEvent);

        assertTrue(listener.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        assertEquals(2, listener.messageNotificationList.size());
        assertEquals(EndpointMessageNotification.MESSAGE_DISPATCH_BEGIN, listener.messageNotificationList.get(0).getAction());
        assertEquals(EndpointMessageNotification.MESSAGE_DISPATCH_END, listener.messageNotificationList.get(1).getAction());
        assertEquals(endpoint.getEndpointURI().getUri().toString(),
            listener.messageNotificationList.get(0).getEndpoint());
                assertEquals(endpoint.getEndpointURI().getUri().toString(),
            listener.messageNotificationList.get(1).getEndpoint());
        assertTrue(listener.messageNotificationList.get(0).getSource() instanceof MuleMessage);
        assertTrue(listener.messageNotificationList.get(1).getSource() instanceof MuleMessage);
        assertEquals(outboundEvent.getMessage().getPayload(),
            listener.messageNotificationList.get(0).getSource().getPayload());
        assertEquals(outboundEvent.getMessage().getPayload(),
            listener.messageNotificationList.get(1).getSource().getPayload());
    }

    @Test
    public void testTransformers() throws Exception
    {
        OutboundEndpoint endpoint = createOutboundEndpoint(null, null, new OutboundAppendTransformer(),
            new ResponseAppendTransformer(), MessageExchangePattern.REQUEST_RESPONSE, null);
        MuleEvent outboundEvent = createTestOutboundEvent();
        MuleEvent result = endpoint.process(outboundEvent);

        assertMessageSent(true);

        assertEquals(TEST_MESSAGE + OutboundAppendTransformer.APPEND_STRING,
        dispacher.sensedSendEvent.getMessageAsString());

        assertNotNull(result);
        assertEquals(RESPONSE_MESSAGE + ResponseAppendTransformer.APPEND_STRING, result.getMessageAsString());
    }

    @Test
    public void testConnectorNotStarted() throws Exception
    {
        OutboundEndpoint endpoint = createOutboundEndpoint(null, null, null, null, 
            MessageExchangePattern.REQUEST_RESPONSE, null);
        testOutboundEvent = createTestOutboundEvent();
        endpoint.getConnector().stop();

        try
        {
            endpoint.process(testOutboundEvent);
            fail("Exception expected");
        }
        catch (MessagingException e)
        {
            // expected
        }
    }

    @Test
    public void testTimeoutSetOnEvent() throws Exception
    {

        int testTimeout = 999;

        OutboundEndpoint endpoint = createOutboundEndpoint(null, null, null, null, 
            MessageExchangePattern.REQUEST_RESPONSE, null);
        testOutboundEvent = createTestOutboundEvent();
        testOutboundEvent.getMessage()
            .setOutboundProperty(MuleProperties.MULE_EVENT_TIMEOUT_PROPERTY, testTimeout);

        endpoint.process(testOutboundEvent);

        assertEquals(testTimeout, dispacher.sensedSendEvent.getTimeout());
    }
    
    @Test
    public void testObjectAwareInjection() throws Exception
    {
        EndpointURIEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(TEST_URI, muleContext);
        endpointBuilder.addMessageProcessor(new ObjectAwareProcessor());

        OutboundEndpoint endpoint = endpointBuilder.buildOutboundEndpoint();
        endpoint.process(createTestOutboundEvent());
        
        ObjectAwareProcessor objectAware = (ObjectAwareProcessor) endpoint.getMessageProcessors().get(0);
        
        assertEquals(muleContext, objectAware.context);
        assertEquals(endpoint, objectAware.endpoint);
    }

    protected MuleEvent assertMessageSent(boolean sync) throws MuleException
    {
        MuleEvent event;
        if (sync)
        {
            assertNull(dispacher.sensedDispatchEvent);
            assertNotNull(dispacher.sensedSendEvent);
            event = dispacher.sensedSendEvent;
        }
        else
        {
            assertNull(dispacher.sensedSendEvent);
            assertNotNull(dispacher.sensedDispatchEvent);
            event = dispacher.sensedDispatchEvent;
        }
        assertNotNull(event.getMessage());
        return event;
    }

    protected MuleEvent assertMessageSentSame(boolean sync) throws MuleException
    {
        MuleEvent event = assertMessageSent(sync);
        if (sync)
        {
            // We can't assert this for async because event gets rewritten
            assertEquals(testOutboundEvent, event);
        }
        assertEquals(TEST_MESSAGE, event.getMessageAsString());
        assertEquals("value1", event.getMessage().getOutboundProperty("prop1"));
        return event;
    }

    protected void assertMessageNotSent() throws MuleException
    {
        assertNull(dispacher.sensedSendEvent);
        assertNull(dispacher.sensedDispatchEvent);
    }

    protected void assertEqualMessages(MuleMessage expect, MuleMessage actual)
    {
        assertEquals(expect.getPayload(), actual.getPayload());
        assertEquals(expect.getEncoding(), actual.getEncoding());
        assertEquals(expect.getUniqueId(), actual.getUniqueId());
        assertEquals(expect.getExceptionPayload(), actual.getExceptionPayload());

        //// Outbound endcodin property is added
        //assertEquals(muleContext.getConfiguration().getDefaultEncoding(),
        //             actual.getOutboundProperty(MuleProperties.MULE_ENCODING_PROPERTY));

    }

    protected OutboundEndpoint createOutboundEndpoint(String uri, Filter filter,
                                                      SecurityFilter securityFilter,
                                                      Transformer in,
                                                      Transformer response,
                                                      MessageExchangePattern exchangePattern,
                                                      TransactionConfig txConfig) throws Exception
    {

        OutboundEndpoint endpoint = createTestOutboundEndpoint(uri, filter, securityFilter, in, response,
            exchangePattern, txConfig);
        dispacher = new FakeMessageDispatcher(endpoint);
        Connector connector = endpoint.getConnector();
        connector.setDispatcherFactory(new TestMessageDispatcherFactory()
        {
            @Override
            public MessageDispatcher create(OutboundEndpoint ep) throws MuleException
            {
                return dispacher;
            }

        });
        return endpoint;
    }

    protected OutboundEndpoint createOutboundEndpoint(Filter filter,
                                                      SecurityFilter securityFilter,
                                                      Transformer in,
                                                      Transformer response,
                                                      MessageExchangePattern exchangePattern,
                                                      TransactionConfig txConfig) throws Exception
    {
        return createOutboundEndpoint("test://test", filter, securityFilter, in, response, exchangePattern, txConfig);

    }

    static class FakeMessageDispatcher extends TestMessageDispatcher
    {
        Latch latch = new Latch();
        MuleEvent sensedSendEvent;
        MuleEvent sensedDispatchEvent;

        public FakeMessageDispatcher(OutboundEndpoint endpoint)
        {
            super(endpoint);
        }

        @Override
        protected MuleMessage doSend(MuleEvent event) throws Exception
        {
            sensedSendEvent = event;
            latch.countDown();
            return responseMessage;
        }

        @Override
        protected void doDispatch(MuleEvent event) throws Exception
        {
            sensedDispatchEvent = event;
            latch.countDown();
        }
    }

}
