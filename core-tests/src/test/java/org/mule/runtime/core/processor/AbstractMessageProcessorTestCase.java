/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.context.MuleContextBuilder;
import org.mule.runtime.core.api.context.notification.EndpointMessageNotificationListener;
import org.mule.runtime.core.api.context.notification.SecurityNotificationListener;
import org.mule.runtime.core.api.endpoint.EndpointBuilder;
import org.mule.runtime.core.api.endpoint.EndpointException;
import org.mule.runtime.core.api.endpoint.ImmutableEndpoint;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.api.security.EndpointSecurityFilter;
import org.mule.runtime.core.api.security.SecurityFilter;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.context.notification.EndpointMessageNotification;
import org.mule.runtime.core.context.notification.SecurityNotification;
import org.mule.runtime.core.context.notification.ServerNotificationManager;
import org.mule.runtime.core.endpoint.EndpointAware;
import org.mule.runtime.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.runtime.core.message.DefaultExceptionPayload;
import org.mule.runtime.core.routing.MessageFilter;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public abstract class AbstractMessageProcessorTestCase extends AbstractMuleContextTestCase
{
    protected static final String TEST_URI = "test://myTestUri";
    protected static String RESPONSE_MESSAGE = "response-message";
    protected static MuleMessage responseMessage;
    protected Answer<MuleEvent> echoEventAnswer = new Answer<MuleEvent>()
    {
        @Override
        public MuleEvent answer(InvocationOnMock invocation) throws Throwable
        {
            return (MuleEvent) invocation.getArguments()[0];
        }
    };


    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        responseMessage = createTestResponseMuleMessage();
        muleContext.start();
    }

    @Override
    protected void configureMuleContext(MuleContextBuilder builder)
    {
        super.configureMuleContext(builder);

        // Configure EndpointMessageNotificationListener for notifications test
        ServerNotificationManager notificationManager = new ServerNotificationManager();
        notificationManager.addInterfaceToType(EndpointMessageNotificationListener.class,
                EndpointMessageNotification.class);
        notificationManager.addInterfaceToType(SecurityNotificationListener.class, SecurityNotification.class);

        builder.setNotificationManager(notificationManager);
    }

    protected InboundEndpoint createTestInboundEndpoint(Transformer transformer,
                                                        Transformer responseTransformer)
            throws EndpointException, InitialisationException
    {
        return createTestInboundEndpoint(null, null, transformer, responseTransformer,
                MessageExchangePattern.REQUEST_RESPONSE, null);
    }

    protected InboundEndpoint createTestInboundEndpoint(Filter filter,
                                                        SecurityFilter securityFilter,
                                                        MessageExchangePattern exchangePattern,
                                                        TransactionConfig txConfig)
            throws InitialisationException, EndpointException
    {
        return createTestInboundEndpoint(filter, securityFilter, null, null, exchangePattern, txConfig);
    }

    protected InboundEndpoint createTestInboundEndpoint(Filter filter,
                                                        SecurityFilter securityFilter,
                                                        Transformer transformer,
                                                        Transformer responseTransformer,
                                                        MessageExchangePattern exchangePattern,
                                                        TransactionConfig txConfig)
            throws EndpointException, InitialisationException
    {
        EndpointURIEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(TEST_URI, muleContext);
        if (filter != null)
        {
            endpointBuilder.addMessageProcessor(new MessageFilter(filter));
        }
        if (securityFilter != null)
        {
            endpointBuilder.addMessageProcessor(new SecurityFilterMessageProcessor(securityFilter));
        }
        if (transformer != null)
        {
            endpointBuilder.setMessageProcessors(Collections.<MessageProcessor> singletonList(transformer));
        }
        if (responseTransformer != null)
        {
            endpointBuilder.setResponseMessageProcessors(Collections.<MessageProcessor> singletonList(responseTransformer));
        }
        endpointBuilder.setExchangePattern(exchangePattern);
        endpointBuilder.setTransactionConfig(txConfig);
        InboundEndpoint endpoint = endpointBuilder.buildInboundEndpoint();
        return endpoint;
    }

    protected MuleEvent createTestInboundEvent(InboundEndpoint endpoint) throws Exception
    {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("prop1", "value1");
        final DefaultMuleEvent event = new DefaultMuleEvent(new DefaultMuleMessage(TEST_MESSAGE, props, muleContext),
                getTestFlow(), getTestSession(null, muleContext));
        event.populateFieldsFromInboundEndpoint(endpoint);
        return event;
    }

    protected OutboundEndpoint createTestOutboundEndpoint(Transformer transformer,
                                                          Transformer responseTransformer)
            throws EndpointException, InitialisationException
    {
        return createTestOutboundEndpoint(null, null, transformer, responseTransformer,
                MessageExchangePattern.REQUEST_RESPONSE, null);
    }

    protected OutboundEndpoint createTestOutboundEndpoint(Filter filter,
                                                          EndpointSecurityFilter securityFilter,
                                                          MessageExchangePattern exchangePattern,
                                                          TransactionConfig txConfig)
            throws InitialisationException, EndpointException
    {
        return createTestOutboundEndpoint(filter, securityFilter, null, null, exchangePattern,
                txConfig);
    }

    protected OutboundEndpoint createTestOutboundEndpoint(Filter filter,
                                                          EndpointSecurityFilter securityFilter,
                                                          Transformer transformer,
                                                          Transformer responseTransformer,
                                                          MessageExchangePattern exchangePattern,
                                                          TransactionConfig txConfig)
            throws EndpointException, InitialisationException
    {
        return createTestOutboundEndpoint("test://test", filter, securityFilter, transformer, responseTransformer, exchangePattern, txConfig);
    }

    protected OutboundEndpoint createTestOutboundEndpoint(String uri, Filter filter,
                                                          SecurityFilter securityFilter,
                                                          Transformer transformer,
                                                          Transformer responseTransformer,
                                                          MessageExchangePattern exchangePattern,
                                                          TransactionConfig txConfig)
            throws EndpointException, InitialisationException
    {
        EndpointURIEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(uri,
                muleContext);
        if (filter != null)
        {
            endpointBuilder.addMessageProcessor(new MessageFilter(filter));
        }
        if (securityFilter != null)
        {
            endpointBuilder.addMessageProcessor(new SecurityFilterMessageProcessor(securityFilter));
        }
        if (transformer != null)
        {
            endpointBuilder.setMessageProcessors(Collections.<MessageProcessor> singletonList(transformer));
        }
        if (responseTransformer != null)
        {
            endpointBuilder.setResponseMessageProcessors(Collections.<MessageProcessor> singletonList(responseTransformer));
        }
        endpointBuilder.setExchangePattern(exchangePattern);
        endpointBuilder.setTransactionConfig(txConfig);
        customizeEndpointBuilder(endpointBuilder);
        return endpointBuilder.buildOutboundEndpoint();
    }

    protected void customizeEndpointBuilder(EndpointBuilder endpointBuilder)
    {
        // template method
    }

    protected MuleEvent createTestOutboundEvent() throws Exception
    {
        return createTestOutboundEvent(null);
    }

    protected MuleEvent createTestOutboundEvent(MessagingExceptionHandler exceptionListener) throws Exception
    {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("prop1", "value1");
        props.put("port", 12345);

        Flow flow = getTestFlow();
        if (exceptionListener != null)
        {
            flow.setExceptionListener(exceptionListener);
        }
        final DefaultMuleEvent event = new DefaultMuleEvent(new DefaultMuleMessage(TEST_MESSAGE, props, muleContext),
                flow, getTestSession(null, muleContext));
        event.populateFieldsFromInboundEndpoint(getTestInboundEndpoint(MessageExchangePattern.REQUEST_RESPONSE));
        return event;
    }

    protected MuleMessage createTestResponseMuleMessage()
    {
        return new DefaultMuleMessage(RESPONSE_MESSAGE, muleContext);
    }

    public static class TestFilter implements Filter
    {
        public boolean accept;

        public TestFilter(boolean accept)
        {
            this.accept = accept;
        }

        @Override
        public boolean accept(MuleMessage message)
        {
            return accept;
        }
    }

    public static class TestSecurityNotificationListener implements SecurityNotificationListener<SecurityNotification>
    {
        public SecurityNotification securityNotification;
        public Latch latch = new Latch();

        @Override
        public void onNotification(SecurityNotification notification)
        {
            securityNotification = notification;
            latch.countDown();
        }
    }

    public static class TestListener implements MessageProcessor
    {
        public MuleEvent sensedEvent;

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            sensedEvent = event;
            return event;
        }
    }

    public static class TestEndpointMessageNotificationListener implements EndpointMessageNotificationListener<EndpointMessageNotification>
    {
        public TestEndpointMessageNotificationListener()
        {
            latchFirst = new CountDownLatch(1);
            latch = new CountDownLatch(1);
        }

        public TestEndpointMessageNotificationListener(int numExpected)
        {
            latchFirst = new CountDownLatch(1);
            latch = new CountDownLatch(numExpected);
        }

        public EndpointMessageNotification messageNotification;
        public List<EndpointMessageNotification> messageNotificationList = new ArrayList<EndpointMessageNotification>();

        public CountDownLatch latchFirst;
        public CountDownLatch latch;

        @Override
        public void onNotification(EndpointMessageNotification notification)
        {
            messageNotification = notification;
            messageNotificationList.add(notification);
            if (latchFirst.getCount() > 0)
            {
                latchFirst.countDown();
            }
            latch.countDown();
        }
    }

    public static class ExceptionThrowingMessageProcessor implements MessageProcessor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            throw new IllegalStateException();
        }
    }

    public static class TestExceptionListener implements MessagingExceptionHandler
    {
        public Exception sensedException;

        @Override
        public MuleEvent handleException(Exception exception, MuleEvent event)
        {
            sensedException = exception;
            event.getMessage().setPayload(NullPayload.getInstance());
            event.getMessage().setExceptionPayload(new DefaultExceptionPayload(exception));
            return event;
        }
    }

    public class ObjectAwareProcessor implements MessageProcessor, EndpointAware, MuleContextAware
    {

        public MuleContext context;
        public ImmutableEndpoint endpoint;

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            return null;
        }

        @Override
        public void setEndpoint(ImmutableEndpoint endpoint)
        {
            this.endpoint = endpoint;
        }

        @Override
        public void setMuleContext(MuleContext context)
        {
            this.context = context;
        }
    }
}
