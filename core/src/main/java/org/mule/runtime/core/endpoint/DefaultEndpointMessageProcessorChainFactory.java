/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.endpoint;

import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.endpoint.EndpointMessageProcessorChainFactory;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.transport.Connector;
import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.runtime.core.endpoint.inbound.InboundEndpointMimeTypeCheckingMessageProcessor;
import org.mule.runtime.core.endpoint.inbound.InboundEndpointPropertyMessageProcessor;
import org.mule.runtime.core.endpoint.inbound.InboundExceptionDetailsMessageProcessor;
import org.mule.runtime.core.endpoint.inbound.InboundLoggingMessageProcessor;
import org.mule.runtime.core.endpoint.inbound.InboundNotificationMessageProcessor;
import org.mule.runtime.core.endpoint.outbound.OutboundEndpointMimeTypeCheckingMessageProcessor;
import org.mule.runtime.core.endpoint.outbound.OutboundEndpointPropertyMessageProcessor;
import org.mule.runtime.core.endpoint.outbound.OutboundEventTimeoutMessageProcessor;
import org.mule.runtime.core.endpoint.outbound.OutboundLoggingMessageProcessor;
import org.mule.runtime.core.endpoint.outbound.OutboundResponsePropertiesMessageProcessor;
import org.mule.runtime.core.endpoint.outbound.OutboundRootMessageIdPropertyMessageProcessor;
import org.mule.runtime.core.endpoint.outbound.OutboundSessionHandlerMessageProcessor;
import org.mule.runtime.core.lifecycle.processor.ProcessIfStartedMessageProcessor;
import org.mule.runtime.core.processor.AbstractRedeliveryPolicy;
import org.mule.runtime.core.processor.EndpointTransactionalInterceptingMessageProcessor;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.routing.requestreply.ReplyToPropertyRequestReplyReplier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefaultEndpointMessageProcessorChainFactory implements EndpointMessageProcessorChainFactory
{
    /** Override this method to change the default MessageProcessors. */
    protected List<MessageProcessor> createInboundMessageProcessors(InboundEndpoint endpoint)
    {
        List<MessageProcessor> list = new ArrayList<MessageProcessor>();

        list.add(new InboundEndpointMimeTypeCheckingMessageProcessor(endpoint));
        list.add(new InboundEndpointPropertyMessageProcessor(endpoint));
        list.add(new InboundNotificationMessageProcessor(endpoint));
        list.add(new InboundLoggingMessageProcessor(endpoint));

        return list;
    }
    
    /** Override this method to change the default MessageProcessors. */
    protected List<MessageProcessor> createInboundResponseMessageProcessors(InboundEndpoint endpoint)
    {
        List<MessageProcessor> list = new ArrayList<MessageProcessor>();

        list.add(new InboundExceptionDetailsMessageProcessor(endpoint.getConnector()));
        list.add(new ReplyToPropertyRequestReplyReplier());
        
        return list;
    }
    
    /** Override this method to change the default MessageProcessors. */
    protected List<MessageProcessor> createOutboundMessageProcessors(OutboundEndpoint endpoint) throws MuleException
    {
        Connector connector = endpoint.getConnector();

        List<MessageProcessor> list = new ArrayList<MessageProcessor>();

        // Log but don't proceed if connector is not started
        list.add(new OutboundLoggingMessageProcessor());
        list.add(new ProcessIfStartedMessageProcessor(connector, connector.getLifecycleState()));

        // Everything is processed within ExecutionTemplate
        list.add(new EndpointTransactionalInterceptingMessageProcessor(endpoint.getTransactionConfig()));

        list.add(new OutboundEventTimeoutMessageProcessor());

        list.add(new OutboundSessionHandlerMessageProcessor(connector.getSessionHandler()));
        list.add(new OutboundEndpointPropertyMessageProcessor(endpoint));
        list.add(new OutboundRootMessageIdPropertyMessageProcessor());
        list.add(new OutboundResponsePropertiesMessageProcessor(endpoint));
        list.add(new OutboundEndpointMimeTypeCheckingMessageProcessor(endpoint));

        return list;
    }
    
    /** Override this method to change the default MessageProcessors. */
    protected List<MessageProcessor> createOutboundResponseMessageProcessors(OutboundEndpoint endpoint) throws MuleException
    {
        return Collections.emptyList();
    }
    
    @Override
    public MessageProcessor createInboundMessageProcessorChain(InboundEndpoint endpoint, FlowConstruct flowConstruct, MessageProcessor target) throws MuleException
    {
        // -- REQUEST CHAIN --
        DefaultMessageProcessorChainBuilder requestChainBuilder = new EndpointMessageProcessorChainBuilder(endpoint, flowConstruct);
        requestChainBuilder.setName("InboundEndpoint '" + endpoint.getEndpointURI().getUri() + "' request chain");
        // Default MPs
        requestChainBuilder.chain(createInboundMessageProcessors(endpoint));
        // Configured MPs (if any)
        AbstractRedeliveryPolicy redeliveryPolicy = endpoint.getRedeliveryPolicy();
        if (redeliveryPolicy != null)
        {
            requestChainBuilder.chain(redeliveryPolicy);
        }
        requestChainBuilder.chain(endpoint.getMessageProcessors());
        
        // -- INVOKE FLOW --
        if (target == null)
        {
            throw new ConfigurationException(MessageFactory.createStaticMessage("No listener (target) has been set for this endpoint"));
        }
        requestChainBuilder.chain(target);

        if (!endpoint.getExchangePattern().hasResponse())
        {
            return requestChainBuilder.build();
        }
        else
        {
            // -- RESPONSE CHAIN --
            DefaultMessageProcessorChainBuilder responseChainBuilder = new EndpointMessageProcessorChainBuilder(endpoint, flowConstruct);
            responseChainBuilder.setName("InboundEndpoint '" + endpoint.getEndpointURI().getUri() + "' response chain");
            // Default MPs
            responseChainBuilder.chain(createInboundResponseMessageProcessors(endpoint));
            // Configured MPs (if any)
            responseChainBuilder.chain(endpoint.getResponseMessageProcessors());

            // -- COMPOSITE REQUEST/RESPONSE CHAIN --
            // Compose request and response chains. We do this so that if the request
            // chain returns early the response chain is still invoked.
            DefaultMessageProcessorChainBuilder compositeChainBuilder = new EndpointMessageProcessorChainBuilder(endpoint, flowConstruct);
            compositeChainBuilder.setName("InboundEndpoint '"+ endpoint.getEndpointURI().getUri() +"' composite request/response chain");
            compositeChainBuilder.chain(requestChainBuilder.build(), responseChainBuilder.build());
            return compositeChainBuilder.build();
        }
    }

    @Override
    public MessageProcessor createOutboundMessageProcessorChain(OutboundEndpoint endpoint, MessageProcessor target) throws MuleException
    {
        // -- REQUEST CHAIN --
        DefaultMessageProcessorChainBuilder requestChainBuilder = new EndpointMessageProcessorChainBuilder(endpoint);
        requestChainBuilder.setName("OutboundEndpoint '" + endpoint.getEndpointURI().getUri() + "' request chain");
        // Default MPs
        requestChainBuilder.chain(createOutboundMessageProcessors(endpoint));
        // Configured MPs (if any)
        requestChainBuilder.chain(endpoint.getMessageProcessors());
        
        // -- INVOKE MESSAGE DISPATCHER --
        if (target == null)
        {
            throw new ConfigurationException(MessageFactory.createStaticMessage("No listener (target) has been set for this endpoint"));
        }
        requestChainBuilder.chain(target);
        
        if (!endpoint.getExchangePattern().hasResponse())
        {
            return requestChainBuilder.build();
        }
        else
        {
            // -- RESPONSE CHAIN --
            DefaultMessageProcessorChainBuilder responseChainBuilder = new EndpointMessageProcessorChainBuilder(endpoint);
            responseChainBuilder.setName("OutboundEndpoint '" + endpoint.getEndpointURI().getUri() + "' response chain");
            // Default MPs
            responseChainBuilder.chain(createOutboundResponseMessageProcessors(endpoint));
            // Configured MPs (if any)
            responseChainBuilder.chain(endpoint.getResponseMessageProcessors());
    
            // Compose request and response chains. We do this so that if the request
            // chain returns early the response chain is still invoked.
            DefaultMessageProcessorChainBuilder compositeChainBuilder = new EndpointMessageProcessorChainBuilder(endpoint);
            compositeChainBuilder.setName("OutboundEndpoint '" + endpoint.getEndpointURI().getUri() + "' composite request/response chain");
            compositeChainBuilder.chain(requestChainBuilder.build(), responseChainBuilder.build());
            return compositeChainBuilder.build();
        }
    }
}

