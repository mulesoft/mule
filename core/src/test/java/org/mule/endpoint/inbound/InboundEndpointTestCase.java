/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.endpoint.inbound;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.filter.Filter;
import org.mule.api.routing.filter.FilterUnacceptedException;
import org.mule.api.security.EndpointSecurityFilter;
import org.mule.context.notification.EndpointMessageNotification;
import org.mule.context.notification.SecurityNotification;
import org.mule.endpoint.AbstractEndpoint;
import org.mule.endpoint.AbstractMessageProcessorTestCase;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.message.DefaultExceptionPayload;
import org.mule.processor.NullMessageProcessor;
import org.mule.tck.security.TestSecurityFilter;
import org.mule.transformer.simple.InboundAppendTransformer;
import org.mule.transformer.simple.ResponseAppendTransformer;

import java.util.HashMap;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
        endpoint = createTestInboundEndpoint(null, null, null, null, 
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
    public void testDefaultFlowAsync() throws Exception
    {
        endpoint = createTestInboundEndpoint(null, null, null, null, 
            MessageExchangePattern.ONE_WAY, null);
        endpoint.setListener(inboundListener);
        requestEvent = createTestRequestEvent(endpoint);
        responseEvent = createTestResponseEvent(endpoint);

        MessageProcessor mpChain = ((AbstractEndpoint) endpoint).getMessageProcessorChain(requestEvent.getFlowConstruct());
        result = mpChain.process(requestEvent);

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

        result = mpChain.process(requestEvent);
        assertNotNull(result);
        assertNotNull("Filter should have thrown a FilterException", result.getMessage().getExceptionPayload());
        assertTrue(result.getMessage().getExceptionPayload().getException() instanceof FilterUnacceptedException);

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
        
        result = mpChain.process(requestEvent);

        assertMessageNotSent();
        assertNotNull(result);
        assertEquals(TestSecurityFilter.SECURITY_EXCEPTION_MESSAGE, result.getMessage().getPayloadAsString());
        assertNotNull(result.getMessage().getExceptionPayload());
        assertTrue(result.getMessage().getExceptionPayload().getException() instanceof TestSecurityFilter.StaticMessageUnauthorisedException);

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

        result = mpChain.process(requestEvent);
        assertNotNull(result);
        assertNotNull("Filter should have thrown a FilterException", result.getMessage().getExceptionPayload());
        assertTrue(result.getMessage().getExceptionPayload().getException() instanceof FilterUnacceptedException);

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
        
        result = mpChain.process(requestEvent);

        assertMessageNotSent();
        assertNotNull(result);
        assertEquals(TestSecurityFilter.SECURITY_EXCEPTION_MESSAGE + ResponseAppendTransformer.APPEND_STRING,
            result.getMessage().getPayloadAsString());
        final int status = result.getMessage().getOutboundProperty("status", 0);
        assertEquals(403, status);

        assertNotNull(result.getMessage().getExceptionPayload());
        assertTrue(result.getMessage().getExceptionPayload().getException() instanceof TestSecurityFilter.StaticMessageUnauthorisedException);
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
        assertEquals(inMessage.getPayload(),
            ((MuleMessage) listener.messageNotification.getSource()).getPayload());
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
            result.getMessage().getPayloadAsString());
    }
    
    @Test
    public void testObjectAwareInjection() throws Exception
    {
        EndpointURIEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(TEST_URI, muleContext);
        endpointBuilder.addMessageProcessor(new ObjectAwareProcessor());

        endpoint = endpointBuilder.buildInboundEndpoint();
        endpoint.setListener(new NullMessageProcessor());
        endpoint.setFlowConstruct(getTestService());
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

    protected MuleEvent createTestRequestEvent(ImmutableEndpoint endpoint) throws Exception
    {
        return new DefaultMuleEvent(inMessage, endpoint, getTestSession(getTestService(), muleContext));
    }
    
    protected MuleEvent createTestResponseEvent(ImmutableEndpoint endpoint) throws Exception
    {
        return new DefaultMuleEvent(new DefaultMuleMessage(RESPONSE_MESSAGE, muleContext),
            endpoint, getTestSession(getTestService(), muleContext));
    }

    protected MuleEvent assertMessageSent(boolean sync) throws MuleException
    {
        MuleEvent event = inboundListener.sensedEvent;
        assertNotNull(event);
        assertEquals(sync, event.getEndpoint().getExchangePattern().hasResponse());
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

    class SensingNullMessageProcessor implements MessageProcessor
    {
        MuleEvent sensedEvent;

        public MuleEvent process(MuleEvent event) throws MuleException
        {
            sensedEvent = event;
            return responseEvent;
        }
    }
}
