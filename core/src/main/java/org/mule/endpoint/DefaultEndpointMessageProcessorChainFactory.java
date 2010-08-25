/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.endpoint;

import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointMessageProcessorChainFactory;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.MessageFactory;
import org.mule.endpoint.inbound.InboundEndpointMimeTypeCheckingMessageProcessor;
import org.mule.endpoint.inbound.InboundEndpointPropertyMessageProcessor;
import org.mule.endpoint.inbound.InboundExceptionDetailsMessageProcessor;
import org.mule.endpoint.inbound.InboundLoggingMessageProcessor;
import org.mule.endpoint.inbound.InboundNotificationMessageProcessor;
import org.mule.endpoint.outbound.OutboundEndpointMimeTypeCheckingMessageProcessor;
import org.mule.endpoint.outbound.OutboundEndpointPropertyMessageProcessor;
import org.mule.endpoint.outbound.OutboundEventTimeoutMessageProcessor;
import org.mule.endpoint.outbound.OutboundLoggingMessageProcessor;
import org.mule.endpoint.outbound.OutboundResponsePropertiesMessageProcessor;
import org.mule.endpoint.outbound.OutboundRewriteResponseEventMessageProcessor;
import org.mule.endpoint.outbound.OutboundSessionHandlerMessageProcessor;
import org.mule.lifecycle.processor.ProcessIfStartedMessageProcessor;
import org.mule.processor.ExceptionHandlingMessageProcessor;
import org.mule.processor.TransactionalInterceptingMessageProcessor;
import org.mule.processor.builder.InterceptingChainMessageProcessorBuilder;
import org.mule.transport.AbstractConnector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DefaultEndpointMessageProcessorChainFactory implements EndpointMessageProcessorChainFactory
{
    /** Override this method to change the default MessageProcessors. */
    protected List<MessageProcessor> createInboundMessageProcessors(InboundEndpoint endpoint)
    {
        return Arrays.asList(new MessageProcessor[] 
        { 
            new ExceptionHandlingMessageProcessor(),
            new InboundEndpointMimeTypeCheckingMessageProcessor(endpoint),
            new InboundEndpointPropertyMessageProcessor(endpoint),
            new InboundNotificationMessageProcessor(endpoint), 
            new InboundLoggingMessageProcessor(endpoint),
        });        
    }
    
    /** Override this method to change the default MessageProcessors. */
    protected List<MessageProcessor> createInboundResponseMessageProcessors(InboundEndpoint endpoint)
    {
        return Arrays.asList(new MessageProcessor[] 
        { 
            new InboundExceptionDetailsMessageProcessor(endpoint.getConnector()),
        });
    }
    
    /** Override this method to change the default MessageProcessors. */
    protected List<MessageProcessor> createOutboundMessageProcessors(OutboundEndpoint endpoint)
        throws MuleException
    {
        // TODO Need to clean this up the class hierarchy, but I'm not sure we want
        // to put all these things in the Connector interface.
        AbstractConnector connector = ((AbstractConnector) endpoint.getConnector());

        List<MessageProcessor> list = new ArrayList<MessageProcessor>();

        list.add(new ExceptionHandlingMessageProcessor());
        
        // Log but don't proceed if connector is not started
        list.add(new OutboundLoggingMessageProcessor());
        list.add(new ProcessIfStartedMessageProcessor(connector, connector.getLifecycleState()));

        // Everything is processed within TransactionTemplate
        list.add(new TransactionalInterceptingMessageProcessor(endpoint.getTransactionConfig()));

        list.add(new OutboundEventTimeoutMessageProcessor());

        list.add(new OutboundSessionHandlerMessageProcessor(connector.getSessionHandler()));
        list.add(new OutboundEndpointPropertyMessageProcessor());
        list.add(new OutboundResponsePropertiesMessageProcessor(endpoint));
        list.add(new OutboundEndpointMimeTypeCheckingMessageProcessor(endpoint));
        return list;
    }
    
    /** Override this method to change the default MessageProcessors. */
    protected List<MessageProcessor> createOutboundResponseMessageProcessors(OutboundEndpoint endpoint)
        throws MuleException
    {
        return Arrays.asList(new MessageProcessor[]{
            new OutboundRewriteResponseEventMessageProcessor()
        });
    }
    
    public MessageProcessor createInboundMessageProcessorChain(InboundEndpoint endpoint, FlowConstruct flowConstruct, MessageProcessor target) throws MuleException
    {
        // -- REQUEST CHAIN --
        InterceptingChainMessageProcessorBuilder requestChainBuilder = new InterceptingChainMessageProcessorBuilder(endpoint, flowConstruct);
        requestChainBuilder.setName("InboundEndpoint '" + endpoint.getEndpointURI().getUri() + "' request chain");
        // Default MPs
        requestChainBuilder.chain(createInboundMessageProcessors(endpoint));
        // Configured MPs (if any)
        requestChainBuilder.chain(endpoint.getMessageProcessors());
        
        // -- INVOKE SERVICE --
        if (target == null)
        {
            throw new ConfigurationException(MessageFactory.createStaticMessage("No listener (target) has been set for this endpoint"));
        }
        requestChainBuilder.chain(target);

        // -- RESPONSE CHAIN --
        InterceptingChainMessageProcessorBuilder responseChainBuilder = new InterceptingChainMessageProcessorBuilder(endpoint, flowConstruct);
        responseChainBuilder.setName("InboundEndpoint '" + endpoint.getEndpointURI().getUri() + "' response chain");
        // Default MPs
        responseChainBuilder.chain(createInboundResponseMessageProcessors(endpoint));
        // Configured MPs (if any)
        responseChainBuilder.chain(endpoint.getResponseMessageProcessors());

        // Compose request and response chains. We do this so that if the request
        // chain returns early the response chain is still invoked.
        InterceptingChainMessageProcessorBuilder compositeChainBuilder = new InterceptingChainMessageProcessorBuilder(endpoint, flowConstruct);
        compositeChainBuilder.setName("InboundEndpoint '"+ endpoint.getEndpointURI().getUri() +"' composite request/response chain");
        compositeChainBuilder.chain(requestChainBuilder.build(), responseChainBuilder.build());
        return compositeChainBuilder.build();
    }

    public MessageProcessor createOutboundMessageProcessorChain(OutboundEndpoint endpoint, FlowConstruct flowConstruct, MessageProcessor target) throws MuleException
    {
        // -- REQUEST CHAIN --
        InterceptingChainMessageProcessorBuilder outboundChainBuilder = new InterceptingChainMessageProcessorBuilder(endpoint, flowConstruct);
        outboundChainBuilder.setName("OutboundEndpoint '" + endpoint.getEndpointURI().getUri() + "' request chain");
        // Default MPs
        outboundChainBuilder.chain(createOutboundMessageProcessors(endpoint));
        // Configured MPs (if any)
        outboundChainBuilder.chain(endpoint.getMessageProcessors());
        
        // -- OUTBOUND ROUTER --
        if (target == null)
        {
            throw new ConfigurationException(MessageFactory.createStaticMessage("No listener (target) has been set for this endpoint"));
        }
        outboundChainBuilder.chain(target);
        
        // -- RESPONSE CHAIN --
        InterceptingChainMessageProcessorBuilder responseChainBuilder = new InterceptingChainMessageProcessorBuilder(endpoint, flowConstruct);
        responseChainBuilder.setName("OutboundEndpoint '" + endpoint.getEndpointURI().getUri() + "' response chain");
        // Default MPs
        responseChainBuilder.chain(createOutboundResponseMessageProcessors(endpoint));
        // Configured MPs (if any)
        responseChainBuilder.chain(endpoint.getResponseMessageProcessors());

        // Compose request and response chains. We do this so that if the request
        // chain returns early the response chain is still invoked.
        InterceptingChainMessageProcessorBuilder compositeChainBuilder = new InterceptingChainMessageProcessorBuilder(endpoint, flowConstruct);
        compositeChainBuilder.setName("OutboundEndpoint '" + endpoint.getEndpointURI().getUri() + "' composite request/response chain");
        compositeChainBuilder.chain(outboundChainBuilder.build(), responseChainBuilder.build());
        return compositeChainBuilder.build();
    }
    
}


