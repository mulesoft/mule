/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport;

import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.MuleContextBuilder;
import org.mule.api.context.notification.EndpointMessageNotificationListener;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.routing.RoutingException;
import org.mule.api.routing.filter.Filter;
import org.mule.api.security.CryptoFailureException;
import org.mule.api.security.EncryptionStrategyNotFoundException;
import org.mule.api.security.EndpointSecurityFilter;
import org.mule.api.security.SecurityException;
import org.mule.api.security.SecurityProviderNotFoundException;
import org.mule.api.security.UnauthorisedException;
import org.mule.api.security.UnknownAuthenticationTypeException;
import org.mule.api.service.Service;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transport.MessageReceiver;
import org.mule.context.notification.EndpointMessageNotification;
import org.mule.context.notification.ServerNotificationManager;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.message.DefaultExceptionPayload;
import org.mule.routing.inbound.DefaultInboundRouterCollection;
import org.mule.routing.response.DefaultResponseRouterCollection;
import org.mule.security.AbstractEndpointSecurityFilter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.mule.TestMessageReceiver;
import org.mule.transaction.MuleTransactionConfig;
import org.mule.util.concurrent.Latch;

import java.util.HashMap;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class MessageReceiverTestCase extends AbstractMuleTestCase
{
    private static final String SECURITY_EXCEPTION_MESSAGE = "unauthorized!!";
    private static final String TEST_MESSAGE = "test";
    private MessageReceiver receiver;
    private FakeDefaultInboundRouterCollection inboundRouterCollection;
    private MuleMessage inMessage;
    private MuleMessage responseMessage;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        inMessage = createTestRequestMessage();
        responseMessage = createTestResponseMuleMessage();
    }

    @Override
    protected void configureMuleContext(MuleContextBuilder builder)
    {
        super.configureMuleContext(builder);

        // Configure EndpointMessageNotificationListener for notifications test
        ServerNotificationManager notificationManager = new ServerNotificationManager();
        notificationManager.addInterfaceToType(EndpointMessageNotificationListener.class,
            EndpointMessageNotification.class);
        builder.setNotificationManager(notificationManager);
    }

    public void testDefaultFlowSync() throws Exception
    {
        receiver = createTestMessageReceiver(null, null, true, null);

        MuleMessage result = receiver.routeMessage(inMessage);

        assertMessageSent(true);
        assertEquals(responseMessage, result);
    }

    public void testDefaultFlowAsync() throws Exception
    {
        receiver = createTestMessageReceiver(null, null, false, null);

        MuleMessage result = receiver.routeMessage(inMessage);

        assertMessageSent(false);
        assertEquals(responseMessage, result);
    }

    public void testFilterAccept() throws Exception
    {
        receiver = createTestMessageReceiver(new TestFilter(true), null, false, null);

        MuleMessage result = receiver.routeMessage(inMessage);

        assertMessageSent(false);
        assertEquals(responseMessage, result);

    }

    public void testFilterNotAccept() throws Exception
    {
        receiver = createTestMessageReceiver(new TestFilter(false), null, false, null);

        MuleMessage result = receiver.routeMessage(inMessage);

        assertMessageNotSent();
        assertEquals(inMessage, result);
        assertNull(result.getExceptionPayload());

    }

    public void testSecurityFilterAccept() throws Exception
    {
        receiver = createTestMessageReceiver(null, new TestSecurityFilter(true), false, null);

        MuleMessage result = receiver.routeMessage(inMessage);

        assertMessageSent(false);
        assertEquals(responseMessage, result);

    }

    public void testSecurityFilterNotAccept() throws Exception
    {
        receiver = createTestMessageReceiver(null, new TestSecurityFilter(false), false, null);

        MuleMessage result = receiver.routeMessage(inMessage);

        assertMessageNotSent();
        assertNotNull(result);
        assertEquals(SECURITY_EXCEPTION_MESSAGE, result.getPayloadAsString());
        assertNotNull(result.getExceptionPayload());
        assertTrue(result.getExceptionPayload().getException() instanceof TestSecurityFilter.FakeUnauthorisedException);

    }

    /**
     * Assert that {@link EndpointSecurityFilter} is only invoked if endpoint
     * {@link Filter} accepts message.
     */
    public void testFilterFirstThenSecurityFilter() throws Exception
    {
        TestSecurityFilter securityFilter = new TestSecurityFilter(false);
        receiver = createTestMessageReceiver(new TestFilter(false), securityFilter, false, null);

        MuleMessage result = receiver.routeMessage(inMessage);

        assertFalse(securityFilter.called);

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
        receiver = createTestMessageReceiver(null, null, false, null);
        inMessage = createTestRequestMessage();
        inMessage.setBooleanProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, true);

        MuleMessage result = receiver.routeMessage(inMessage);

        assertMessageSent(true);
        assertEquals(responseMessage, result);
    }

    /**
     * Assert that event is sync if transaction is used even if endpoint is async
     */
    public void testTransactionForcesSync() throws Exception
    {
        TransactionConfig txConfig = new MuleTransactionConfig();
        txConfig.setAction(TransactionConfig.ACTION_ALWAYS_BEGIN);
        receiver = createTestMessageReceiver(null, null, false, txConfig);
        inMessage.setBooleanProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, true);

        MuleMessage result = receiver.routeMessage(inMessage);

        assertMessageSent(true);
        assertEquals(responseMessage, result);
    }

    public void testMessagePropertyErrorMapping() throws Exception
    {
        receiver = createTestMessageReceiver(null, null, false, null);
        responseMessage.setExceptionPayload(new DefaultExceptionPayload(new RuntimeException()));

        MuleMessage result = receiver.routeMessage(inMessage);

        assertMessageSent(false);
        assertEquals(responseMessage, result);
        assertEquals(500, result.getIntProperty("status", 0));

    }

    public void testResponseRouterFlow() throws Exception
    {
        InboundEndpoint endpoint = createTestEndpoint(null, null, false, null);
        Service service = createTestService();
        FakeDefaultResponseRouterCollection responseRouterCollection = new FakeDefaultResponseRouterCollection();
        responseRouterCollection.addEndpoint(endpoint);
        service.setResponseRouter(responseRouterCollection);
        receiver = new TestMessageReceiver(endpoint.getConnector(), service, endpoint);
        receiver.initialise();

        MuleMessage result = receiver.routeMessage(inMessage);

        assertMessageNotSent();
        assertNull(result);
        assertNotNull(responseRouterCollection.sensedEvent);
    }

    public void testNotfication() throws Exception
    {
        final Latch latch = new Latch();
        muleContext.registerListener(new EndpointMessageNotificationListener<EndpointMessageNotification>()
        {

            public void onNotification(EndpointMessageNotification notification)
            {
                if (notification instanceof EndpointMessageNotification)
                {
                    latch.countDown();
                }
            }
        });

        receiver = createTestMessageReceiver(null, null, false, null);
        receiver.routeMessage(createTestRequestMessage());
        assertTrue(latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));

    }

    protected MessageReceiver createTestMessageReceiver(Filter filter,
                                                        EndpointSecurityFilter securityFilter,
                                                        boolean sync,
                                                        TransactionConfig txConfig) throws Exception
    {
        InboundEndpoint endpoint = createTestEndpoint(filter, securityFilter, sync, txConfig);
        receiver = new TestMessageReceiver(endpoint.getConnector(), createTestService(), endpoint);
        receiver.initialise();
        return receiver;
    }

    private Service createTestService() throws Exception
    {
        Service service = getTestService();
        inboundRouterCollection = new FakeDefaultInboundRouterCollection();
        service.setInboundRouter(inboundRouterCollection);
        return service;
    }

    private InboundEndpoint createTestEndpoint(Filter filter,
                                               EndpointSecurityFilter securityFilter,
                                               boolean sync,
                                               TransactionConfig txConfig)
        throws EndpointException, InitialisationException
    {
        EndpointURIEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder("test://test",
            muleContext);
        endpointBuilder.setFilter(filter);
        endpointBuilder.setSecurityFilter(securityFilter);
        endpointBuilder.setSynchronous(sync);
        endpointBuilder.setTransactionConfig(txConfig);
        InboundEndpoint endpoint = endpointBuilder.buildInboundEndpoint();
        return endpoint;
    }

    protected MuleMessage createTestRequestMessage()
    {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("prop1", "value1");
        return new DefaultMuleMessage(TEST_MESSAGE, props, muleContext);
    }

    protected MuleMessage createTestResponseMuleMessage()
    {
        return new DefaultMuleMessage("response", muleContext);
    }

    protected MuleEvent assertMessageSent(boolean sync) throws MuleException
    {
        MuleEvent event = inboundRouterCollection.sensedEvent;
        assertNotNull(event);
        assertEquals(sync, event.isSynchronous());
        assertNotNull(event.getMessage());
        assertEquals(inMessage, event.getMessage());
        assertEquals(TEST_MESSAGE, event.getMessageAsString());
        assertEquals("value1", event.getMessage().getProperty("prop1"));
        return event;
    }

    protected void assertMessageNotSent() throws MuleException
    {
        MuleEvent event = inboundRouterCollection.sensedEvent;
        assertNull(event);
    }

    class FakeDefaultInboundRouterCollection extends DefaultInboundRouterCollection
    {
        MuleEvent sensedEvent;

        @Override
        public MuleMessage route(MuleEvent event) throws MessagingException
        {
            sensedEvent = event;
            return responseMessage;
        }
    }

    class FakeDefaultResponseRouterCollection extends DefaultResponseRouterCollection
    {
        MuleEvent sensedEvent;

        @Override
        public void route(MuleEvent event) throws RoutingException
        {
            this.sensedEvent = event;
        }
    }

    private static class TestFilter implements Filter
    {
        boolean accept;

        public TestFilter(boolean accept)
        {
            this.accept = accept;
        }

        public boolean accept(MuleMessage message)
        {
            return accept;
        }
    }

    private static class TestSecurityFilter extends AbstractEndpointSecurityFilter
    {
        boolean accept;
        boolean called;

        public TestSecurityFilter(boolean accept)
        {
            this.accept = accept;
        }

        @Override
        protected void authenticateInbound(MuleEvent event)
            throws SecurityException, CryptoFailureException, SecurityProviderNotFoundException,
            EncryptionStrategyNotFoundException, UnknownAuthenticationTypeException
        {
            called = true;
            if (!accept)
            {
                throw new FakeUnauthorisedException();
            }
        }

        @Override
        protected void authenticateOutbound(MuleEvent event)
            throws SecurityException, SecurityProviderNotFoundException, CryptoFailureException
        {
            if (!accept)
            {
                throw new FakeUnauthorisedException();
            }
        }

        @Override
        protected void doInitialise() throws InitialisationException
        {

        }

        class FakeUnauthorisedException extends UnauthorisedException
        {
            public FakeUnauthorisedException()
            {
                super(null);
            }

            @Override
            public String getLocalizedMessage()
            {
                return SECURITY_EXCEPTION_MESSAGE;
            }
        }
    }

}
