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
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.filter.Filter;
import org.mule.api.security.EndpointSecurityFilter;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.MessageReceiver;
import org.mule.context.notification.EndpointMessageNotification;
import org.mule.context.notification.SecurityNotification;
import org.mule.message.DefaultExceptionPayload;
import org.mule.tck.security.TestSecurityFilter;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.tck.testmodels.mule.TestMessageReceiver;
import org.mule.transaction.MuleTransactionConfig;
import org.mule.transformer.simple.InboundAppendTransformer;
import org.mule.transformer.simple.ResponseAppendTransformer;

import java.util.HashMap;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class InboundEndpointTestCase extends AbstractInboundMessageProcessorTestCase
{
    private static final String TEST_MESSAGE = "test";
    private MessageReceiver receiver;
    private SensingNullMessageProcessor inboundListener;
    private MuleMessage inMessage;
    private MuleEvent responseEvent;

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
        receiver = createTestMessageReceiver(null, null, null, null, true, null);
        responseEvent = createTestResponseEvent(receiver);

        MuleMessage result = receiver.routeMessage(inMessage);

        assertMessageSentSame(true);
        assertEquals(responseEvent.getMessage(), result);
    }

    public void testDefaultFlowAsync() throws Exception
    {
        receiver = createTestMessageReceiver(null, null, null, null, false, null);
        responseEvent = createTestResponseEvent(receiver);

        MuleMessage result = receiver.routeMessage(inMessage);

        assertMessageSentSame(false);
        assertEquals(responseEvent.getMessage(), result);
    }

    public void testFilterAccept() throws Exception
    {
        receiver = createTestMessageReceiver(new TestFilter(true), null, null, null, false, null);
        responseEvent = createTestResponseEvent(receiver);

        MuleMessage result = receiver.routeMessage(inMessage);

        assertMessageSentSame(false);
        assertEquals(responseEvent.getMessage(), result);

    }

    public void testFilterNotAccept() throws Exception
    {
        receiver = createTestMessageReceiver(new TestFilter(false), null, null, null, false, null);
        responseEvent = createTestResponseEvent(receiver);

        MuleMessage result = receiver.routeMessage(inMessage);

        assertMessageNotSent();
        assertEquals(inMessage, result);
        assertNull(result.getExceptionPayload());

    }

    public void testSecurityFilterAccept() throws Exception
    {
        receiver = createTestMessageReceiver(null, new TestSecurityFilter(true), null, null, false, null);
        responseEvent = createTestResponseEvent(receiver);

        MuleMessage result = receiver.routeMessage(inMessage);

        assertMessageSentSame(false);
        assertEquals(responseEvent.getMessage(), result);

    }

    public void testSecurityFilterNotAccept() throws Exception
    {
        TestSecurityNotificationListener securityNotificationListener = new TestSecurityNotificationListener();
        muleContext.registerListener(securityNotificationListener);

        receiver = createTestMessageReceiver(null, new TestSecurityFilter(false), null, null, false, null);
        responseEvent = createTestResponseEvent(receiver);

        MuleMessage result = receiver.routeMessage(inMessage);

        assertMessageNotSent();
        assertNotNull(result);
        assertEquals(TestSecurityFilter.SECURITY_EXCEPTION_MESSAGE, result.getPayloadAsString());
        assertNotNull(result.getExceptionPayload());
        assertTrue(result.getExceptionPayload().getException() instanceof TestSecurityFilter.StaticMessageUnauthorisedException);

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
        receiver = createTestMessageReceiver(new TestFilter(false), securityFilter, null, null, false, null);
        responseEvent = createTestResponseEvent(receiver);

        MuleMessage result = receiver.routeMessage(inMessage);

        assertFalse(securityFilter.wasCalled());

        assertMessageNotSent();
        assertEquals(inMessage, result);
        assertNull(result.getExceptionPayload());
    }

    /**
     * Assert that event is sync even if endpoint is async when remoteSync message
     * property is set
     */
    public void testRemoteSyncMessagePropertyForcesSync() throws Exception
    {
        receiver = createTestMessageReceiver(null, null, null, null, false, null);
        responseEvent = createTestResponseEvent(receiver);

        inMessage = createTestRequestMessage();
        inMessage.setBooleanProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, true);

        MuleMessage result = receiver.routeMessage(inMessage);

        assertMessageSentSame(true);
        assertEquals(responseEvent.getMessage(), result);
    }

    /**
     * Assert that event is sync if transaction is used even if endpoint is async
     */
    public void testTransactionForcesSync() throws Exception
    {
        TransactionConfig txConfig = new MuleTransactionConfig();
        txConfig.setAction(TransactionConfig.ACTION_ALWAYS_BEGIN);
        receiver = createTestMessageReceiver(null, null, null, null, false, txConfig);
        responseEvent = createTestResponseEvent(receiver);

        inMessage.setBooleanProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, true);

        MuleMessage result = receiver.routeMessage(inMessage);

        assertMessageSentSame(true);
        assertEquals(responseEvent.getMessage(), result);
    }

    public void testMessagePropertyErrorMapping() throws Exception
    {
        receiver = createTestMessageReceiver(null, null, null, null, false, null);
        responseEvent = createTestResponseEvent(receiver);
        responseEvent.getMessage().setExceptionPayload(new DefaultExceptionPayload(new RuntimeException()));

        MuleMessage result = receiver.routeMessage(inMessage);

        assertMessageSentSame(false);
        assertEquals(responseEvent.getMessage(), result);
        assertEquals(500, result.getIntProperty("status", 0));
    }

    public void testResponseTransformerExceptionDetailAfterRequestFlowInterupt() throws Exception
    {
        receiver = createTestMessageReceiver(null, new TestSecurityFilter(false), null,
            new ResponseAppendTransformer(), false, null);
        responseEvent = createTestResponseEvent(receiver);

        responseEvent.getMessage().setExceptionPayload(new DefaultExceptionPayload(new RuntimeException()));

        MuleMessage result = receiver.routeMessage(inMessage);

        assertMessageNotSent();
        assertNotNull(result);
        assertEquals(TestSecurityFilter.SECURITY_EXCEPTION_MESSAGE + ResponseAppendTransformer.APPEND_STRING,
            result.getPayloadAsString());
        assertEquals(403, result.getIntProperty("status", 0));

        assertNotNull(result.getExceptionPayload());
        assertTrue(result.getExceptionPayload().getException() instanceof TestSecurityFilter.StaticMessageUnauthorisedException);
    }

    public void testNotfication() throws Exception
    {
        TestEndpointMessageNotificationListener<EndpointMessageNotification> listener = new TestEndpointMessageNotificationListener<EndpointMessageNotification>();
        muleContext.registerListener(listener);

        receiver = createTestMessageReceiver(null, null, null, null, false, null);
        responseEvent = createTestResponseEvent(receiver);

        receiver.routeMessage(createTestRequestMessage());
        assertTrue(listener.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        assertEquals(EndpointMessageNotification.MESSAGE_RECEIVED, listener.messageNotification.getAction());
        assertEquals(receiver.getEndpoint().getEndpointURI().getUri().toString(),
            listener.messageNotification.getEndpoint());
        assertTrue(listener.messageNotification.getSource() instanceof MuleMessage);
        assertEquals(inMessage.getPayload(),
            ((MuleMessage) listener.messageNotification.getSource()).getPayload());
    }

    public void testTransformers() throws Exception
    {
        receiver = createTestMessageReceiver(null, null, new InboundAppendTransformer(),
            new ResponseAppendTransformer(), true, null);
        responseEvent = createTestResponseEvent(receiver);

        MuleMessage response = receiver.routeMessage(createTestRequestMessage());

        assertMessageSent(true);
        assertEquals(TEST_MESSAGE + InboundAppendTransformer.APPEND_STRING,
            inboundListener.sensedEvent.getMessageAsString());

        assertNotNull(response);
        assertEquals(RESPONSE_MESSAGE + ResponseAppendTransformer.APPEND_STRING,
            response.getPayloadAsString());
    }

    protected MessageReceiver createTestMessageReceiver(Filter filter,
                                                        EndpointSecurityFilter securityFilter,
                                                        Transformer in,
                                                        Transformer response,
                                                        boolean sync,
                                                        TransactionConfig txConfig) throws Exception
    {
        InboundEndpoint endpoint = createTestInboundEndpoint(filter, securityFilter, in, response, sync,
            txConfig);
        TestConnector connector = (TestConnector) endpoint.getConnector();
        receiver = new TestMessageReceiver(connector, getTestService(), endpoint);
        receiver.setListener(connector.createInboundEndpointMessageProcessorChain(endpoint, inboundListener));
        receiver.initialise();
        return receiver;
    }

    protected MuleMessage createTestRequestMessage()
    {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("prop1", "value1");
        return new DefaultMuleMessage(TEST_MESSAGE, props, muleContext);
    }

    protected MuleEvent createTestResponseEvent(MessageReceiver receiver) throws Exception
    {
        return new DefaultMuleEvent(new DefaultMuleMessage(RESPONSE_MESSAGE, muleContext),
            receiver.getEndpoint(), getTestSession(getTestService(), muleContext), true);
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
