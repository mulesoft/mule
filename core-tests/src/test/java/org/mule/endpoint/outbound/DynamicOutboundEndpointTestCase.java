/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.endpoint.outbound;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.context.notification.EndpointMessageNotification.MESSAGE_DISPATCH_BEGIN;
import static org.mule.context.notification.EndpointMessageNotification.MESSAGE_DISPATCH_END;
import static org.mule.context.notification.EndpointMessageNotification.MESSAGE_SEND_BEGIN;
import static org.mule.context.notification.EndpointMessageNotification.MESSAGE_SEND_END;

import org.mule.MessageExchangePattern;
import org.mule.VoidMuleEvent;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.registry.ServiceException;
import org.mule.api.registry.ServiceType;
import org.mule.api.routing.filter.Filter;
import org.mule.api.security.SecurityFilter;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.MessageDispatcher;
import org.mule.context.notification.SecurityNotification;
import org.mule.endpoint.AbstractMessageProcessorTestCase;
import org.mule.endpoint.DynamicOutboundEndpoint;
import org.mule.endpoint.DynamicURIBuilder;
import org.mule.endpoint.URIBuilder;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.tck.security.TestSecurityFilter;
import org.mule.tck.testmodels.mule.TestMessageDispatcher;
import org.mule.tck.testmodels.mule.TestMessageDispatcherFactory;
import org.mule.transformer.simple.OutboundAppendTransformer;
import org.mule.transformer.simple.ResponseAppendTransformer;
import org.mule.transport.service.DefaultTransportServiceDescriptor;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.Closure;
import org.apache.commons.collections.functors.NOPClosure;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests flow of messages from {@link org.mule.endpoint.DynamicOutboundEndpoint#process(org.mule.api.MuleEvent)} down to
 * {@link org.mule.transport.AbstractMessageDispatcher} and the chain of MessageProcessor's that
 * implement the outbound endpoint processing.
 */
public class DynamicOutboundEndpointTestCase extends AbstractMessageProcessorTestCase
{

    private MuleEvent testOutboundEvent;

    @Before
    public void setCurrentTestInstance() throws ServiceException
    {
        overrideDispatcherInServiceDescriptor();
        MyMessageDispatcherFactory.dispatcher = null;
    }

    @Before
    public void clearAssertionClosures()
    {
        MyMessageDispatcherFactory.afterSend = NOPClosure.getInstance();
        MyMessageDispatcherFactory.afterDispatch = NOPClosure.getInstance();
    }

    @Test
    public void testDefaultFlowRequestResponse() throws Exception
    {
        OutboundEndpoint endpoint = createOutboundEndpoint(null, null, null, null, MessageExchangePattern.REQUEST_RESPONSE, null);
        testOutboundEvent = createTestOutboundEvent();

        MuleEvent result = endpoint.process(testOutboundEvent);

        assertEventSent();

        assertSame(responseMessage, result.getMessage());

        assertEqualMessages(responseMessage, result.getMessage());
    }

    @Test
    public void testDefaultFlowOneWay() throws Exception
    {
        OutboundEndpoint endpoint = createOutboundEndpoint(null, null, null, null, MessageExchangePattern.ONE_WAY, null);
        assertTrue(endpoint instanceof DynamicOutboundEndpoint);

        testOutboundEvent = createTestOutboundEvent();

        MuleEvent result = endpoint.process(testOutboundEvent);

        assertEventDispatched();
        assertSame(VoidMuleEvent.getInstance(), result);
        assertMessageSentEqual(MyMessageDispatcherFactory.dispatcher.sensedDispatchEvent);
    }

    @Test
    public void testSecurityFilterAccept() throws Exception
    {
        OutboundEndpoint endpoint = createOutboundEndpoint(null, new TestSecurityFilter(true), null, null, MessageExchangePattern.REQUEST_RESPONSE, null);
        testOutboundEvent = createTestOutboundEvent();

        MuleEvent result = endpoint.process(testOutboundEvent);

        assertEventSent();
        assertMessageSentEqual(MyMessageDispatcherFactory.dispatcher.sensedSendEvent);

        assertSame(responseMessage, result.getMessage());

        assertEqualMessages(responseMessage, result.getMessage());
    }

    @Test
    public void testSecurityFilterNotAccept() throws Exception
    {
        TestSecurityNotificationListener securityNotificationListener = new TestSecurityNotificationListener();
        muleContext.registerListener(securityNotificationListener);

        OutboundEndpoint endpoint = createOutboundEndpoint(null, new TestSecurityFilter(false), null, null, MessageExchangePattern.REQUEST_RESPONSE, null);
        testOutboundEvent = createTestOutboundEvent();

        try
        {
            endpoint.process(testOutboundEvent);
            fail("Exception expected");
        }
        catch (TestSecurityFilter.StaticMessageUnauthorisedException e)
        {
            testOutboundEvent.getFlowConstruct().getExceptionListener().handleException(e, testOutboundEvent);
        }

        assertNull(MyMessageDispatcherFactory.dispatcher);

        assertTrue(securityNotificationListener.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        assertEquals(SecurityNotification.SECURITY_AUTHENTICATION_FAILED,
                     securityNotificationListener.securityNotification.getAction());
        assertEquals(securityNotificationListener.securityNotification.getResourceIdentifier(),
                     TestSecurityFilter.StaticMessageUnauthorisedException.class.getName());
    }

    @Test
    public void testSendNotification() throws Exception
    {
        final TestEndpointMessageNotificationListener listener = new TestEndpointMessageNotificationListener(2);
        muleContext.registerListener(listener);

        final MuleEvent outboundEvent = createTestOutboundEvent();

        OutboundEndpoint endpoint = createOutboundEndpoint(null, null, null, null, MessageExchangePattern.REQUEST_RESPONSE, null);

        MyMessageDispatcherFactory.afterSend = new Closure()
        {
            @Override
            public void execute(Object input)
            {
                try
                {
                    assertTrue(listener.latchFirst.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
                }
                catch (InterruptedException e)
                {
                    fail(e.getMessage());
                }
                assertThat(listener.messageNotificationList, hasSize(1));
                assertThat(listener.messageNotificationList.get(0).getAction(), is(MESSAGE_SEND_BEGIN));
                assertThat(listener.messageNotificationList.get(0).getSource(), instanceOf(MuleMessage.class));
                assertThat(listener.messageNotificationList.get(0).getSource().getPayload(),
                        is(outboundEvent.getMessage().getPayload()));
            }
        };

        endpoint.process(outboundEvent);

        assertEventSent();
        assertTrue(listener.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        assertThat(listener.messageNotificationList, hasSize(2));
        assertThat(listener.messageNotificationList.get(0).getAction(), is(MESSAGE_SEND_BEGIN));
        assertThat(listener.messageNotificationList.get(1).getAction(), is(MESSAGE_SEND_END));
        assertThat(listener.messageNotificationList.get(0).getSource(), instanceOf(MuleMessage.class));
        assertThat(listener.messageNotificationList.get(1).getSource(), instanceOf(MuleMessage.class));
        assertThat(listener.messageNotificationList.get(0).getSource().getPayload(),
                is(outboundEvent.getMessage().getPayload()));
        assertThat(listener.messageNotificationList.get(1).getSource().getPayload(),
                is((Object) RESPONSE_MESSAGE));
    }

    @Test
    public void testDispatchNotification() throws Exception
    {
        final TestEndpointMessageNotificationListener listener = new TestEndpointMessageNotificationListener(2);
        muleContext.registerListener(listener);

        OutboundEndpoint endpoint = createOutboundEndpoint(null, null, null, null, MessageExchangePattern.ONE_WAY, null);
        final MuleEvent outboundEvent = createTestOutboundEvent();

        MyMessageDispatcherFactory.afterDispatch = new Closure()
        {
            @Override
            public void execute(Object input)
            {
                try
                {
                    assertTrue(listener.latchFirst.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
                }
                catch (InterruptedException e)
                {
                    fail(e.getMessage());
                }
                assertThat(listener.messageNotificationList, hasSize(1));
                assertThat(listener.messageNotificationList.get(0).getAction(), is(MESSAGE_DISPATCH_BEGIN));
                assertThat(listener.messageNotificationList.get(0).getSource(), instanceOf(MuleMessage.class));
                assertThat(listener.messageNotificationList.get(0).getSource().getPayload(),
                        is(outboundEvent.getMessage().getPayload()));
            }
        };

        endpoint.process(outboundEvent);

        assertEventDispatched();
        assertTrue(listener.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        assertThat(listener.messageNotificationList, hasSize(2));
        assertThat(listener.messageNotificationList.get(0).getAction(), is(MESSAGE_DISPATCH_BEGIN));
        assertThat(listener.messageNotificationList.get(1).getAction(), is(MESSAGE_DISPATCH_END));
        assertThat(listener.messageNotificationList.get(0).getSource(), instanceOf(MuleMessage.class));
        assertThat(listener.messageNotificationList.get(1).getSource(), instanceOf(MuleMessage.class));
        assertThat(listener.messageNotificationList.get(0).getSource().getPayload(),
                is(outboundEvent.getMessage().getPayload()));
        assertThat(listener.messageNotificationList.get(1).getSource().getPayload(),
                is(outboundEvent.getMessage().getPayload()));
    }

    @Test
    public void testTransformers() throws Exception
    {
        OutboundEndpoint endpoint = createOutboundEndpoint(null, null, new OutboundAppendTransformer(),
                                                           new ResponseAppendTransformer(), MessageExchangePattern.REQUEST_RESPONSE, null);

        testOutboundEvent = createTestOutboundEvent();

        MuleEvent result = endpoint.process(testOutboundEvent);

        assertNotNull(result);
        assertEquals(TEST_MESSAGE + OutboundAppendTransformer.APPEND_STRING, MyMessageDispatcherFactory.dispatcher.sensedSendEvent.getMessageAsString());
        assertEquals(RESPONSE_MESSAGE + ResponseAppendTransformer.APPEND_STRING, result.getMessageAsString());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testConnectorNotAvailableOnDynamicEndpoint() throws Exception
    {
        OutboundEndpoint endpoint = createOutboundEndpoint(null, null, null, null, MessageExchangePattern.REQUEST_RESPONSE, null);

        endpoint.getConnector();
    }

    @Test
    public void testTimeoutSetOnEvent() throws Exception
    {
        int testTimeout = 999;

        OutboundEndpoint endpoint = createOutboundEndpoint(null, null, null, null, MessageExchangePattern.REQUEST_RESPONSE, null);

        testOutboundEvent = createTestOutboundEvent();
        testOutboundEvent.getMessage().setOutboundProperty(MuleProperties.MULE_EVENT_TIMEOUT_PROPERTY, testTimeout);

        MuleEvent response = endpoint.process(testOutboundEvent);

        assertEquals(testTimeout, response.getTimeout());
    }

    @Test
    public void cachesResolvedStaticEndpoints() throws Exception
    {
        OutboundEndpoint prototypeEndpoint = mock(OutboundEndpoint.class);
        when(prototypeEndpoint.getMuleContext()).thenReturn(muleContext);

        EndpointBuilder staticEndpointBuilder = mock(EndpointBuilder.class);
        when(staticEndpointBuilder.buildOutboundEndpoint()).thenReturn(prototypeEndpoint);

        EndpointBuilder endpointBuilder = mock(EndpointBuilder.class);
        when(endpointBuilder.buildOutboundEndpoint()).thenReturn(prototypeEndpoint);
        when(endpointBuilder.clone()).thenReturn(staticEndpointBuilder);

        DynamicOutboundEndpoint dynamicOutboundEndpoint = new DynamicOutboundEndpoint(endpointBuilder, new DynamicURIBuilder(new URIBuilder("test://localhost:#[message.outboundProperties.port]", muleContext)));

        testOutboundEvent = createTestOutboundEvent();
        dynamicOutboundEndpoint.process(testOutboundEvent);
        dynamicOutboundEndpoint.process(testOutboundEvent);

        verify(endpointBuilder, times(1)).buildOutboundEndpoint();
    }

    protected void assertMessageSentEqual(MuleEvent event) throws MuleException
    {
        assertEquals(TEST_MESSAGE, event.getMessageAsString());
        assertEquals("value1", event.getMessage().getOutboundProperty("prop1"));
    }

    protected void assertEqualMessages(MuleMessage expect, MuleMessage actual)
    {
        assertEquals(expect.getPayload(), actual.getPayload());
        assertEquals(expect.getEncoding(), actual.getEncoding());
        assertEquals(expect.getUniqueId(), actual.getUniqueId());
        assertEquals(expect.getExceptionPayload(), actual.getExceptionPayload());
    }

    private void assertEventDispatched()
    {
        Prober prober = new PollingProber();
        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                return MyMessageDispatcherFactory.dispatcher != null && MyMessageDispatcherFactory.dispatcher.dispatchedEvent;
            }

            @Override
            public String describeFailure()
            {
                return "Expected dispatcher was not called";
            }
        });

        assertNull(MyMessageDispatcherFactory.dispatcher.sensedSendEvent);
        assertNotNull(MyMessageDispatcherFactory.dispatcher.sensedDispatchEvent);
        assertNotNull(MyMessageDispatcherFactory.dispatcher.sensedDispatchEvent.getMessage());
    }

    private void assertEventSent()
    {
        Prober prober = new PollingProber();
        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                return MyMessageDispatcherFactory.dispatcher != null && MyMessageDispatcherFactory.dispatcher.sentEvent;
            }

            @Override
            public String describeFailure()
            {
                return "Expected dispatcher was not called";
            }
        });

        assertNull(MyMessageDispatcherFactory.dispatcher.sensedDispatchEvent);
        assertNotNull(MyMessageDispatcherFactory.dispatcher.sensedSendEvent);
        assertNotNull(MyMessageDispatcherFactory.dispatcher.sensedSendEvent.getMessage());
    }

    private void overrideDispatcherInServiceDescriptor() throws ServiceException
    {
        Properties props = new Properties();
        props.put(MuleProperties.CONNECTOR_DISPATCHER_FACTORY, MyMessageDispatcherFactory.class.getName());
        DefaultTransportServiceDescriptor serviceDescriptor = (DefaultTransportServiceDescriptor) muleContext.getRegistry().lookupServiceDescriptor(ServiceType.TRANSPORT, "test", null);
        props.put(MuleProperties.CONNECTOR_INBOUND_EXCHANGE_PATTERNS, "ONE_WAY, REQUEST_RESPONSE");
        props.put(MuleProperties.CONNECTOR_OUTBOUND_EXCHANGE_PATTERNS, "ONE_WAY, REQUEST_RESPONSE");
        serviceDescriptor.setOverrides(props);
    }

    protected OutboundEndpoint createOutboundEndpoint(Filter filter,
                                                      SecurityFilter securityFilter,
                                                      Transformer in,
                                                      Transformer response,
                                                      MessageExchangePattern exchangePattern,
                                                      TransactionConfig txConfig) throws Exception
    {

        return createTestOutboundEndpoint("test://localhost:#[message.outboundProperties.port]", filter, securityFilter, in, response,
                                          exchangePattern, txConfig);
    }

    private static class FakeMessageDispatcher extends TestMessageDispatcher
    {

        private final Closure afterSend;
        private final Closure afterDispatch;

        private MuleEvent sensedSendEvent;
        private MuleEvent sensedDispatchEvent;
        private boolean sentEvent;
        private boolean dispatchedEvent;

        public FakeMessageDispatcher(OutboundEndpoint endpoint, Closure afterSend, Closure afterDispatch)
        {
            super(endpoint);
            this.afterSend = afterSend;
            this.afterDispatch = afterDispatch;
        }

        @Override
        protected MuleMessage doSend(MuleEvent event) throws Exception
        {
            sensedSendEvent = event;
            sentEvent = true;
            afterSend.execute(event);
            return responseMessage;
        }

        @Override
        protected void doDispatch(MuleEvent event) throws Exception
        {
            sensedDispatchEvent = event;
            dispatchedEvent = true;
            afterDispatch.execute(event);
        }
    }

    public static class MyMessageDispatcherFactory extends TestMessageDispatcherFactory
    {

        static FakeMessageDispatcher dispatcher;

        static Closure afterSend;
        static Closure afterDispatch;

        @Override
        public synchronized MessageDispatcher create(OutboundEndpoint ep) throws MuleException
        {
            if (dispatcher != null)
            {
                throw new IllegalStateException("Dispatcher for this test was already created");
            }

            dispatcher = new FakeMessageDispatcher(ep, afterSend, afterDispatch);
            return dispatcher;
        }
    }
}
