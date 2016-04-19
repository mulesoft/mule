/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.endpoint.inbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.RequestContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.api.routing.filter.FilterUnacceptedException;
import org.mule.runtime.core.api.security.EndpointSecurityFilter;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.context.notification.EndpointMessageNotification;
import org.mule.runtime.core.context.notification.SecurityNotification;
import org.mule.runtime.core.endpoint.AbstractEndpoint;
import org.mule.runtime.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.runtime.core.message.DefaultExceptionPayload;
import org.mule.runtime.core.processor.AbstractMessageProcessorTestCase;
import org.mule.runtime.core.processor.NullMessageProcessor;
import org.mule.runtime.core.transformer.simple.InboundAppendTransformer;
import org.mule.runtime.core.transformer.simple.ResponseAppendTransformer;
import org.mule.tck.security.TestSecurityFilter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.Mockito;

public class InboundEndpointTestCase extends AbstractMessageProcessorTestCase
{
    private static final String TEST_MESSAGE = "test";
    private InboundEndpoint endpoint;
    private SensingNullMessageProcessor inboundListener;
    private MuleMessage inMessage;
    private MuleEvent requestEvent;
    private MuleEvent responseEvent;
    private MuleEvent result;

    private static String RESPONSE_MESSAGE = "response-message";

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        inMessage = createTestRequestMessage();
        inboundListener = new SensingNullMessageProcessor();
    }

    @Test
    public void testDefaultFlowSync() throws Exception
    {
        Transformer reqTransformer = mock(Transformer.class);
        Mockito.when(reqTransformer.process(Mockito.any(MuleEvent.class))).then(echoEventAnswer);
        Transformer resTransformer = mock(Transformer.class);
        Mockito.when(resTransformer.process(Mockito.any(MuleEvent.class))).then(echoEventAnswer);

        endpoint = createTestInboundEndpoint(null, null, reqTransformer, resTransformer,
            MessageExchangePattern.REQUEST_RESPONSE, null);
        endpoint.setListener(inboundListener);
        requestEvent = createTestRequestEvent(endpoint);
        responseEvent = createTestResponseEvent(endpoint);

        MessageProcessor mpChain = ((AbstractEndpoint) endpoint).getMessageProcessorChain(requestEvent.getFlowConstruct());
        result = mpChain.process(requestEvent);
        
        Mockito.verify(reqTransformer, Mockito.times(1)).process(Mockito.any(MuleEvent.class));
        Mockito.verify(resTransformer, Mockito.times(1)).process(Mockito.any(MuleEvent.class));

        assertMessageSentSame(true);
        assertEquals(responseEvent.getMessage(), result.getMessage());
    }

    @Test
    public void testDefaultFlowAsync() throws Exception
    {
        Transformer reqTransformer = mock(Transformer.class);
        Mockito.when(reqTransformer.process(Mockito.any(MuleEvent.class))).then(echoEventAnswer);
        Transformer resTransformer = mock(Transformer.class);
        Mockito.when(resTransformer.process(Mockito.any(MuleEvent.class))).then(echoEventAnswer);

        endpoint = createTestInboundEndpoint(null, null, reqTransformer, resTransformer,
            MessageExchangePattern.ONE_WAY, null);
        endpoint.setListener(inboundListener);
        requestEvent = createTestRequestEvent(endpoint);
        responseEvent = createTestResponseEvent(endpoint);

        MessageProcessor mpChain = ((AbstractEndpoint) endpoint).getMessageProcessorChain(requestEvent.getFlowConstruct());
        result = mpChain.process(requestEvent);
        
        Mockito.verify(reqTransformer, Mockito.times(1)).process(Mockito.any(MuleEvent.class));
        Mockito.verify(resTransformer, Mockito.never()).process(Mockito.any(MuleEvent.class));

        assertMessageSentSame(false);
        assertEquals(responseEvent.getMessage(), result.getMessage());
    }

    @Test
    public void testFilterAccept() throws Exception
    {
        endpoint = createTestInboundEndpoint(new TestFilter(true), null, null, null,
            MessageExchangePattern.REQUEST_RESPONSE, null);
        endpoint.setListener(inboundListener);
        requestEvent = createTestRequestEvent(endpoint);
        responseEvent = createTestResponseEvent(endpoint);

        MessageProcessor mpChain = ((AbstractEndpoint) endpoint).getMessageProcessorChain(requestEvent.getFlowConstruct());
        result = mpChain.process(requestEvent);

        assertMessageSentSame(true);
        assertEquals(responseEvent.getMessage(), result.getMessage());

    }

    @Test
    public void testFilterNotAccept() throws Exception
    {
        endpoint = createTestInboundEndpoint(new TestFilter(false), null, null, null,
            MessageExchangePattern.REQUEST_RESPONSE, null);
        endpoint.setListener(inboundListener);
        requestEvent = createTestRequestEvent(endpoint);
        responseEvent = createTestResponseEvent(endpoint);

        MessageProcessor mpChain = ((AbstractEndpoint) endpoint).getMessageProcessorChain(requestEvent.getFlowConstruct());

        try
        {
            result = mpChain.process(requestEvent);
            fail("Filter should have thrown a FilterException");
        }
        catch (FilterUnacceptedException e)
        {
            // expected
        }

        assertMessageNotSent();
    }

    @Test
    public void testSecurityFilterAccept() throws Exception
    {
        endpoint = createTestInboundEndpoint(null, new TestSecurityFilter(true), null, null,
            MessageExchangePattern.REQUEST_RESPONSE, null);
        endpoint.setListener(inboundListener);
        requestEvent = createTestRequestEvent(endpoint);
        responseEvent = createTestResponseEvent(endpoint);

        MessageProcessor mpChain = ((AbstractEndpoint) endpoint).getMessageProcessorChain(requestEvent.getFlowConstruct());
        result = mpChain.process(requestEvent);

        assertMessageSentSame(true);
        assertEquals(responseEvent.getMessage(), result.getMessage());

    }

    @Test
    public void testSecurityFilterNotAccept() throws Exception
    {
        TestSecurityNotificationListener securityNotificationListener = new TestSecurityNotificationListener();
        muleContext.registerListener(securityNotificationListener);

        endpoint = createTestInboundEndpoint(null, new TestSecurityFilter(false), null, null,
            MessageExchangePattern.REQUEST_RESPONSE, null);
        endpoint.setListener(inboundListener);
        requestEvent = createTestRequestEvent(endpoint);
        responseEvent = createTestResponseEvent(endpoint);

        MessageProcessor mpChain = ((AbstractEndpoint) endpoint).getMessageProcessorChain(requestEvent.getFlowConstruct());

        // Required for UnauthorisedException creation
        RequestContext.setEvent(requestEvent);

        try
        {
            result = mpChain.process(requestEvent);
            fail("Exception expected");
        }
        catch (TestSecurityFilter.StaticMessageUnauthorisedException e)
        {
            requestEvent.getFlowConstruct().getExceptionListener().handleException(e, requestEvent);
        }

        assertTrue(securityNotificationListener.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        assertEquals(SecurityNotification.SECURITY_AUTHENTICATION_FAILED,
            securityNotificationListener.securityNotification.getAction());
        assertEquals(securityNotificationListener.securityNotification.getResourceIdentifier(),
            TestSecurityFilter.StaticMessageUnauthorisedException.class.getName());
    }

    /**
     * Assert that {@link EndpointSecurityFilter} is only invoked if endpoint
     * {@link Filter} accepts message.
     */
    @Test
    public void testFilterFirstThenSecurityFilter() throws Exception
    {
        TestSecurityFilter securityFilter = new TestSecurityFilter(false);
        endpoint = createTestInboundEndpoint(new TestFilter(false), securityFilter, null, null,
            MessageExchangePattern.REQUEST_RESPONSE, null);
        endpoint.setListener(inboundListener);
        requestEvent = createTestRequestEvent(endpoint);
        responseEvent = createTestResponseEvent(endpoint);

        MessageProcessor mpChain = ((AbstractEndpoint) endpoint).getMessageProcessorChain(requestEvent.getFlowConstruct());

        try
        {
            result = mpChain.process(requestEvent);
            fail("Filter should have thrown a FilterException");
        }
        catch (FilterUnacceptedException e)
        {
            // expected
        }

        assertFalse(securityFilter.wasCalled());
        assertMessageNotSent();
    }

    @Test
    public void testMessagePropertyErrorMapping() throws Exception
    {
        endpoint = createTestInboundEndpoint(null, null, null, null,
            MessageExchangePattern.REQUEST_RESPONSE, null);
        endpoint.setListener(inboundListener);
        requestEvent = createTestRequestEvent(endpoint);
        responseEvent = createTestResponseEvent(endpoint);
        responseEvent.getMessage().setExceptionPayload(new DefaultExceptionPayload(new RuntimeException()));

        MessageProcessor mpChain = ((AbstractEndpoint) endpoint).getMessageProcessorChain(requestEvent.getFlowConstruct());
        result = mpChain.process(requestEvent);

        assertMessageSentSame(true);
        assertEquals(responseEvent.getMessage(), result.getMessage());
        final int status = result.getMessage().getOutboundProperty("status", 0);
        assertEquals(500, status);
    }

    @Test
    public void testResponseTransformerExceptionDetailAfterRequestFlowInterupt() throws Exception
    {
        endpoint = createTestInboundEndpoint(null, new TestSecurityFilter(false), null,
            new ResponseAppendTransformer(), MessageExchangePattern.REQUEST_RESPONSE, null);
        endpoint.setListener(inboundListener);
        requestEvent = createTestRequestEvent(endpoint);
        responseEvent = createTestResponseEvent(endpoint);
        responseEvent.getMessage().setExceptionPayload(new DefaultExceptionPayload(new RuntimeException()));

        MessageProcessor mpChain = ((AbstractEndpoint) endpoint).getMessageProcessorChain(requestEvent.getFlowConstruct());

        // Required for UnauthorisedException creation
        RequestContext.setEvent(requestEvent);

        try
        {
            result = mpChain.process(requestEvent);
            fail("Exception expected");
        }
        catch (TestSecurityFilter.StaticMessageUnauthorisedException e)
        {
            // expected
        }

        assertMessageNotSent();
    }

    @Test
    public void testNotfication() throws Exception
    {
        TestEndpointMessageNotificationListener listener = new TestEndpointMessageNotificationListener();
        muleContext.registerListener(listener);

        endpoint = createTestInboundEndpoint(null, null, null, null,
            MessageExchangePattern.REQUEST_RESPONSE, null);
        endpoint.setListener(inboundListener);
        requestEvent = createTestRequestEvent(endpoint);
        responseEvent = createTestResponseEvent(endpoint);

        MessageProcessor mpChain = ((AbstractEndpoint) endpoint).getMessageProcessorChain(requestEvent.getFlowConstruct());
        result = mpChain.process(requestEvent);

        assertTrue(listener.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        assertEquals(EndpointMessageNotification.MESSAGE_RECEIVED, listener.messageNotification.getAction());
        assertEquals(endpoint.getEndpointURI().getUri().toString(),
            listener.messageNotification.getEndpoint());
        assertTrue(listener.messageNotification.getSource() instanceof MuleMessage);
        assertEquals(inMessage.getPayload(), listener.messageNotification.getSource().getPayload());
    }

    @Test
    public void testTransformers() throws Exception
    {
        endpoint = createTestInboundEndpoint(null, null, new InboundAppendTransformer(),
            new ResponseAppendTransformer(), MessageExchangePattern.REQUEST_RESPONSE, null);
        endpoint.setListener(inboundListener);
        requestEvent = createTestRequestEvent(endpoint);
        responseEvent = createTestResponseEvent(endpoint);

        MessageProcessor mpChain = ((AbstractEndpoint) endpoint).getMessageProcessorChain(requestEvent.getFlowConstruct());
        result = mpChain.process(requestEvent);

        assertMessageSent(true);
        assertEquals(TEST_MESSAGE + InboundAppendTransformer.APPEND_STRING,
            inboundListener.sensedEvent.getMessageAsString());

        assertNotNull(result);
        assertEquals(RESPONSE_MESSAGE + ResponseAppendTransformer.APPEND_STRING,
            getPayloadAsString(result.getMessage()));
    }

    @Test
    public void testObjectAwareInjection() throws Exception
    {
        EndpointURIEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(TEST_URI, muleContext);
        endpointBuilder.addMessageProcessor(new ObjectAwareProcessor());

        endpoint = endpointBuilder.buildInboundEndpoint();
        endpoint.setListener(new NullMessageProcessor());
        endpoint.setFlowConstruct(getTestFlow());
        endpoint.start();

        ObjectAwareProcessor objectAware = (ObjectAwareProcessor) endpoint.getMessageProcessors().get(0);

        assertEquals(muleContext, objectAware.context);
        assertEquals(endpoint, objectAware.endpoint);

        endpoint.stop();
    }

    protected MuleMessage createTestRequestMessage()
    {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("prop1", "value1");
        return new DefaultMuleMessage(TEST_MESSAGE, props, muleContext);
    }

    protected MuleEvent createTestRequestEvent(InboundEndpoint ep) throws Exception
    {
        final DefaultMuleEvent event = new DefaultMuleEvent(inMessage, getTestFlow(), getTestSession(null, muleContext));
        event.populateFieldsFromInboundEndpoint(ep);
        return event;
    }

    protected MuleEvent createTestResponseEvent(InboundEndpoint ep) throws Exception
    {
        final DefaultMuleEvent event = new DefaultMuleEvent(new DefaultMuleMessage(RESPONSE_MESSAGE, muleContext),
                getTestFlow(), getTestSession(null, muleContext));
        event.populateFieldsFromInboundEndpoint(ep);
        return event;
    }

    protected MuleEvent assertMessageSent(boolean sync) throws MuleException
    {
        MuleEvent event = inboundListener.sensedEvent;
        assertNotNull(event);
        assertEquals(sync, event.getExchangePattern().hasResponse());
        assertNotNull(event.getMessage());
        return event;
    }

    protected MuleEvent assertMessageSentSame(boolean sync) throws MuleException
    {
        assertMessageSent(sync);
        MuleEvent event = inboundListener.sensedEvent;
        assertEquals(inMessage, event.getMessage());
        assertEquals(TEST_MESSAGE, event.getMessageAsString());
        assertEquals("value1", event.getMessage().getOutboundProperty("prop1"));
        return event;
    }

    protected void assertMessageNotSent() throws MuleException
    {
        MuleEvent event = inboundListener.sensedEvent;
        assertNull(event);
    }

    private class SensingNullMessageProcessor implements MessageProcessor
    {
        MuleEvent sensedEvent;

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            sensedEvent = event;
            return responseEvent;
        }
    }
}
