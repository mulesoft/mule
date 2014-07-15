/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.endpoint;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.endpoint.OutboundEndpointExecutorFactory;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.processor.MessageProcessor;
import org.mule.processor.LaxAsyncInterceptingMessageProcessor;
import org.mule.processor.chain.SimpleMessageProcessorChainBuilder;
import org.mule.transport.AbstractConnector;
import org.mule.transport.DispatcherWorkManagerSource;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.collections.map.LRUMap;

public class DefaultOutboundEndpointExecutorFactory implements OutboundEndpointExecutorFactory, MuleContextAware
{

    private MuleContext muleContext;

    @Override
    public OutboundEndpoint getOutboundEndpointExecutor(final OutboundEndpoint outboundEndpoint, final MessagingExceptionHandler messagingExceptionHandler) throws MuleException
    {
        if (outboundEndpoint instanceof DynamicOutboundEndpoint)
        {
            return createLaxInterceptingMessageProcessor((DynamicOutboundEndpoint) outboundEndpoint, messagingExceptionHandler);
        }
        else
        {
            return createLaxInterceptingMessageProcessor(outboundEndpoint, muleContext, messagingExceptionHandler);
        }
    }

    private static OutboundEndpoint createLaxInterceptingMessageProcessor(final OutboundEndpoint endpoint, final MuleContext muleContext, final MessagingExceptionHandler messagingExceptionHandler) throws MuleException
    {
        final AbstractConnector endpointConnector = (AbstractConnector) endpoint.getConnector();
        MessageProcessor executionMessageProcessor;
        if (endpoint.getExchangePattern().hasResponse() || !endpointConnector.getDispatcherThreadingProfile().isDoThreading())
        {
            executionMessageProcessor = endpoint;
        }
        else
        {
            SimpleMessageProcessorChainBuilder builder = new SimpleMessageProcessorChainBuilder();
            builder.setName("dispatcher processor chain for '" + endpoint.getAddress() + "'");
            LaxAsyncInterceptingMessageProcessor async = new LaxAsyncInterceptingMessageProcessor(new DispatcherWorkManagerSource(endpointConnector));
            async.setMuleContext(muleContext);
            async.setMessagingExceptionHandler(messagingExceptionHandler);
            builder.chain(async);
            builder.chain(endpoint);
            executionMessageProcessor = builder.build();
        }
        return createOutboundEndpointWrapper(executionMessageProcessor, endpoint);
    }

    private static OutboundEndpoint createOutboundEndpointWrapper(final MessageProcessor executionMessageProcessor, final OutboundEndpoint endpoint)
    {
        return new OutboundEndpointWrapper()
        {
            @Override
            protected MessageProcessor getExecutionMessageProcessor(MuleEvent event)
            {
                return executionMessageProcessor;
            }

            @Override
            protected OutboundEndpoint getOutboundEndpoint()
            {
                return endpoint;
            }
        };
    }

    private OutboundEndpoint createLaxInterceptingMessageProcessor(final DynamicOutboundEndpoint endpoint, final MessagingExceptionHandler messagingExceptionHandler)
    {
        return new DeferredLaxInterceptionMessageProcessor(endpoint, messagingExceptionHandler);
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    private class DeferredLaxInterceptionMessageProcessor extends OutboundEndpointWrapper implements MessageProcessor
    {

        private final Map<OutboundEndpoint, MessageProcessor> laxInterceptingMessageProcessorCache = Collections.synchronizedMap(new LRUMap(64));
        private final DynamicOutboundEndpoint dynamicOutboundEndpoint;
        private MessagingExceptionHandler messagingExceptionHandler;

        public DeferredLaxInterceptionMessageProcessor(final DynamicOutboundEndpoint dynamicOutboundEndpoint, final MessagingExceptionHandler messagingExceptionHandler)
        {
            this.dynamicOutboundEndpoint = dynamicOutboundEndpoint;
            this.messagingExceptionHandler = messagingExceptionHandler;
        }

        @Override
        protected MessageProcessor getExecutionMessageProcessor(MuleEvent event) throws MuleException
        {
            final OutboundEndpoint staticEndpoint = dynamicOutboundEndpoint.getStaticEndpoint(event);
            MessageProcessor messageProcessor = laxInterceptingMessageProcessorCache.get(staticEndpoint);
            if (messageProcessor == null)
            {
                messageProcessor = createLaxInterceptingMessageProcessor(staticEndpoint, muleContext, messagingExceptionHandler);
                laxInterceptingMessageProcessorCache.put(staticEndpoint, messageProcessor);
            }
            return messageProcessor;
        }

        @Override
        protected OutboundEndpoint getOutboundEndpoint()
        {
            return dynamicOutboundEndpoint;
        }

    }
}
