/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.endpoint.inbound;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.filter.Filter;
import org.mule.api.security.EndpointSecurityFilter;
import org.mule.api.transaction.TransactionConfig;
import org.mule.context.notification.EndpointMessageNotification;
import org.mule.context.notification.SecurityNotification;
import org.mule.message.DefaultExceptionPayload;
import org.mule.tck.security.TestSecurityFilter;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.transaction.MuleTransactionConfig;
import org.mule.transformer.simple.InboundAppendTransformer;
import org.mule.transformer.simple.ResponseAppendTransformer;

import java.util.HashMap;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class InboundEndpointTestCase extends AbstractInboundMessageProcessorTestCase
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

    public void testDefaultFlowSync() throws Exception
    {
        endpoint = createTestInboundEndpoint(null, null, null, null, true, null);
        requestEvent = createTestRequestEvent(endpoint);
        responseEvent = createTestResponseEvent(endpoint);

        MessageProcessor mpChain = createInboundEndpointMessageProcessorChain(endpoint, inboundListener);
        result = mpChain.process(requestEvent);

        assertMessageSentSame(true);
        assertEquals(responseEvent.getMessage(), result.getMessage());
    }

    public void testDefaultFlowAsync() throws Exception
    {
        endpoint = createTestInboundEndpoint(null, null, null, null, false, null);
        requestEvent = createTestRequestEvent(endpoint);
        responseEvent = createTestResponseEvent(endpoint);

        MessageProcessor mpChain = createInboundEndpointMessageProcessorChain(endpoint, inboundListener);
        result = mpChain.process(requestEvent);

        assertMessageSentSame(false);
        assertEquals(responseEvent.getMessage(), result.getMessage());
    }

    public void testFilterAccept() throws Exception
    {
        endpoint = createTestInboundEndpoint(new TestFilter(true), null, null, null, false, null);
        requestEvent = createTestRequestEvent(endpoint);
        responseEvent = createTestResponseEvent(endpoint);

        MessageProcessor mpChain = createInboundEndpointMessageProcessorChain(endpoint, inboundListener);
        result = mpChain.process(requestEvent);

        assertMessageSentSame(false);
        assertEquals(responseEvent.getMessage(), result.getMessage());

    }

    public void testFilterNotAccept() throws Exception
    {
        endpoint = createTestInboundEndpoint(new TestFilter(false), null, null, null, false, null);
        requestEvent = createTestRequestEvent(endpoint);
        responseEvent = createTestResponseEvent(endpoint);

        MessageProcessor mpChain = createInboundEndpointMessageProcessorChain(endpoint, inboundListener);
        result = mpChain.process(requestEvent);

        assertMessageNotSent();
        assertEquals(inMessage, result.getMessage());
        assertNull(result.getMessage().getExceptionPayload());

    }

    public void testSecurityFilterAccept() throws Exception
    {
        endpoint = createTestInboundEndpoint(null, new TestSecurityFilter(true), null, null, false, null);
        requestEvent = createTestRequestEvent(endpoint);
        responseEvent = createTestResponseEvent(endpoint);

        MessageProcessor mpChain = createInboundEndpointMessageProcessorChain(endpoint, inboundListener);
        result = mpChain.process(requestEvent);

        assertMessageSentSame(false);
        assertEquals(responseEvent.getMessage(), result.getMessage());

    }

    public void testSecurityFilterNotAccept() throws Exception
    {
        TestSecurityNotificationListener securityNotificationListener = new TestSecurityNotificationListener();
        muleContext.registerListener(securityNotificationListener);

        endpoint = createTestInboundEndpoint(null, new TestSecurityFilter(false), null, null, false, null);
        requestEvent = createTestRequestEvent(endpoint);
        responseEvent = createTestResponseEvent(endpoint);

        MessageProcessor mpChain = createInboundEndpointMessageProcessorChain(endpoint, inboundListener);
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
    public void testFilterFirstThenSecurityFilter() throws Exception
    {
        TestSecurityFilter securityFilter = new TestSecurityFilter(false);
        endpoint = createTestInboundEndpoint(new TestFilter(false), securityFilter, null, null, false, null);
        requestEvent = createTestRequestEvent(endpoint);
        responseEvent = createTestResponseEvent(endpoint);

        MessageProcessor mpChain = createInboundEndpointMessageProcessorChain(endpoint, inboundListener);
        result = mpChain.process(requestEvent);

        assertFalse(securityFilter.wasCalled());

        assertMessageNotSent();
        assertEquals(inMessage, result.getMessage());
        assertNull(result.getMessage().getExceptionPayload());
    }

    /**
     * Assert that event is sync even if endpoint is async when remoteSync message
     * property is set
     */
    public void testRemoteSyncMessagePropertyForcesSync() throws Exception
    {
        endpoint = createTestInboundEndpoint(null, null, null, null, false, null);

        inMessage.setBooleanProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, true);
        requestEvent = createTestRequestEvent(endpoint);
        responseEvent = createTestResponseEvent(endpoint);

        MessageProcessor mpChain = createInboundEndpointMessageProcessorChain(endpoint, inboundListener);
        result = mpChain.process(requestEvent);

        assertMessageSentSame(true);
        assertEquals(responseEvent.getMessage(), result.getMessage());
    }

    /**
     * Assert that event is sync if transaction is used even if endpoint is async
     */
    public void testTransactionForcesSync() throws Exception
    {
        TransactionConfig txConfig = new MuleTransactionConfig();
        txConfig.setAction(TransactionConfig.ACTION_ALWAYS_BEGIN);
        endpoint = createTestInboundEndpoint(null, null, null, null, false, txConfig);

        inMessage.setBooleanProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, true);
        requestEvent = createTestRequestEvent(endpoint);
        responseEvent = createTestResponseEvent(endpoint);

        MessageProcessor mpChain = createInboundEndpointMessageProcessorChain(endpoint, inboundListener);
        result = mpChain.process(requestEvent);

        assertMessageSentSame(true);
        assertEquals(responseEvent.getMessage(), result.getMessage());
    }

    public void testMessagePropertyErrorMapping() throws Exception
    {
        endpoint = createTestInboundEndpoint(null, null, null, null, false, null);
        requestEvent = createTestRequestEvent(endpoint);
        responseEvent = createTestResponseEvent(endpoint);
        responseEvent.getMessage().setExceptionPayload(new DefaultExceptionPayload(new RuntimeException()));

        MessageProcessor mpChain = createInboundEndpointMessageProcessorChain(endpoint, inboundListener);
        result = mpChain.process(requestEvent);

        assertMessageSentSame(false);
        assertEquals(responseEvent.getMessage(), result.getMessage());
        assertEquals(500, result.getMessage().getIntProperty("status", 0));
    }

    public void testResponseTransformerExceptionDetailAfterRequestFlowInterupt() throws Exception
    {
        endpoint = createTestInboundEndpoint(null, new TestSecurityFilter(false), null,
            new ResponseAppendTransformer(), false, null);
        requestEvent = createTestRequestEvent(endpoint);
        responseEvent = createTestResponseEvent(endpoint);
        responseEvent.getMessage().setExceptionPayload(new DefaultExceptionPayload(new RuntimeException()));

        MessageProcessor mpChain = createInboundEndpointMessageProcessorChain(endpoint, inboundListener);
        result = mpChain.process(requestEvent);

        assertMessageNotSent();
        assertNotNull(result);
        assertEquals(TestSecurityFilter.SECURITY_EXCEPTION_MESSAGE + ResponseAppendTransformer.APPEND_STRING,
            result.getMessage().getPayloadAsString());
        assertEquals(403, result.getMessage().getIntProperty("status", 0));

        assertNotNull(result.getMessage().getExceptionPayload());
        assertTrue(result.getMessage().getExceptionPayload().getException() instanceof TestSecurityFilter.StaticMessageUnauthorisedException);
    }

    public void testNotfication() throws Exception
    {
        TestEndpointMessageNotificationListener<EndpointMessageNotification> listener = new TestEndpointMessageNotificationListener<EndpointMessageNotification>();
        muleContext.registerListener(listener);

        endpoint = createTestInboundEndpoint(null, null, null, null, false, null);
        requestEvent = createTestRequestEvent(endpoint);
        responseEvent = createTestResponseEvent(endpoint);

        MessageProcessor mpChain = createInboundEndpointMessageProcessorChain(endpoint, inboundListener);
        result = mpChain.process(requestEvent);

        assertTrue(listener.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        assertEquals(EndpointMessageNotification.MESSAGE_RECEIVED, listener.messageNotification.getAction());
        assertEquals(endpoint.getEndpointURI().getUri().toString(),
            listener.messageNotification.getEndpoint());
        assertTrue(listener.messageNotification.getSource() instanceof MuleMessage);
        assertEquals(inMessage.getPayload(),
            ((MuleMessage) listener.messageNotification.getSource()).getPayload());
    }

    public void testTransformers() throws Exception
    {
        endpoint = createTestInboundEndpoint(null, null, new InboundAppendTransformer(),
            new ResponseAppendTransformer(), true, null);
        requestEvent = createTestRequestEvent(endpoint);
        responseEvent = createTestResponseEvent(endpoint);

        MessageProcessor mpChain = createInboundEndpointMessageProcessorChain(endpoint, inboundListener);
        result = mpChain.process(requestEvent);

        assertMessageSent(true);
        assertEquals(TEST_MESSAGE + InboundAppendTransformer.APPEND_STRING,
            inboundListener.sensedEvent.getMessageAsString());

        assertNotNull(result);
        assertEquals(RESPONSE_MESSAGE + ResponseAppendTransformer.APPEND_STRING,
            result.getMessage().getPayloadAsString());
    }

    protected MessageProcessor createInboundEndpointMessageProcessorChain(InboundEndpoint endpoint,
                                                                          MessageProcessor listener)
    {
        return ((TestConnector) endpoint.getConnector()).createInboundEndpointMessageProcessorChain(endpoint, listener);
    }

    protected MuleMessage createTestRequestMessage()
    {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("prop1", "value1");
        return new DefaultMuleMessage(TEST_MESSAGE, props, muleContext);
    }

    protected MuleEvent createTestRequestEvent(ImmutableEndpoint endpoint) throws Exception
    {
        boolean synchronous;
        if (inMessage.getBooleanProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, false))
        {
            synchronous = true;
        }
        else
        {
            synchronous = endpoint.isSynchronous();
        }
        return new DefaultMuleEvent(inMessage, endpoint, getTestSession(getTestService(), muleContext), synchronous);
    }
    
    protected MuleEvent createTestResponseEvent(ImmutableEndpoint endpoint) throws Exception
    {
        return new DefaultMuleEvent(new DefaultMuleMessage(RESPONSE_MESSAGE, muleContext),
            endpoint, getTestSession(getTestService(), muleContext), true);
    }

    protected MuleEvent assertMessageSent(boolean sync) throws MuleException
    {
        MuleEvent event = inboundListener.sensedEvent;
        assertNotNull(event);
        assertEquals(sync, event.isSynchronous());
        assertNotNull(event.getMessage());
        return event;
    }

    protected MuleEvent assertMessageSentSame(boolean sync) throws MuleException
    {
        assertMessageSent(sync);
        MuleEvent event = inboundListener.sensedEvent;
        assertEquals(inMessage, event.getMessage());
        assertEquals(TEST_MESSAGE, event.getMessageAsString());
        assertEquals("value1", event.getMessage().getProperty("prop1"));
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
