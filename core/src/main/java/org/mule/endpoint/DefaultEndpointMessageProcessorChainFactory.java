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
import org.mule.api.endpoint.EndpointMessageProcessorChainFactory;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.endpoint.OutboundEndpointDecorator;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.MessageFactory;
import org.mule.endpoint.inbound.InboundEndpointPropertyMessageProcessor;
import org.mule.endpoint.inbound.InboundExceptionDetailsMessageProcessor;
import org.mule.endpoint.inbound.InboundFilterMessageProcessor;
import org.mule.endpoint.inbound.InboundLoggingMessageProcessor;
import org.mule.endpoint.inbound.InboundNotificationMessageProcessor;
import org.mule.endpoint.inbound.InboundSecurityFilterMessageProcessor;
import org.mule.endpoint.outbound.OutboundEndpointDecoratorMessageProcessor;
import org.mule.endpoint.outbound.OutboundEndpointPropertyMessageProcessor;
import org.mule.endpoint.outbound.OutboundEventTimeoutMessageProcessor;
import org.mule.endpoint.outbound.OutboundLoggingMessageProcessor;
import org.mule.endpoint.outbound.OutboundResponsePropertiesMessageProcessor;
import org.mule.endpoint.outbound.OutboundRewriteResponseEventMessageProcessor;
import org.mule.endpoint.outbound.OutboundSecurityFilterMessageProcessor;
import org.mule.endpoint.outbound.OutboundSessionHandlerMessageProcessor;
import org.mule.endpoint.outbound.OutboundSimpleTryCatchMessageProcessor;
import org.mule.endpoint.outbound.OutboundTryCatchMessageProcessor;
import org.mule.lifecycle.processor.ProcessIfStartedMessageProcessor;
import org.mule.processor.TransactionalInterceptingMessageProcessor;
import org.mule.processor.builder.ChainMessageProcessorBuilder;
import org.mule.transformer.TransformerMessageProcessor;
import org.mule.transport.AbstractConnector;

public class DefaultEndpointMessageProcessorChainFactory implements EndpointMessageProcessorChainFactory
{
    /** Override this method to change the default MessageProcessors. */
    protected MessageProcessor[] createInboundMessageProcessors(InboundEndpoint endpoint)
    {
        return new MessageProcessor[] 
        { 
            new InboundEndpointPropertyMessageProcessor(endpoint),
            new InboundNotificationMessageProcessor(endpoint), 
            new InboundLoggingMessageProcessor(endpoint),
            new InboundFilterMessageProcessor(), 
            new InboundSecurityFilterMessageProcessor(endpoint),
            new TransformerMessageProcessor(endpoint.getTransformers()) 
        };
    }
    
    /** Override this method to change the default MessageProcessors. */
    protected MessageProcessor[] createInboundResponseMessageProcessors(InboundEndpoint endpoint)
    {
        return new MessageProcessor[] 
        { 
            new InboundExceptionDetailsMessageProcessor(endpoint.getConnector()),
            new TransformerMessageProcessor(endpoint.getResponseTransformers()) 
        };
    }
    
    /** Override this method to change the default MessageProcessors. */
    protected MessageProcessor[] createOutboundMessageProcessors(OutboundEndpoint endpoint) throws MuleException
    {
        // TODO Need to clean this up the class hierarchy, but I'm not sure we want to put all these things in the Connector interface.
        AbstractConnector connector = ((AbstractConnector) endpoint.getConnector());
        
        return new MessageProcessor[] 
        { 
            // Log but don't proceed if connector is not started
            new OutboundLoggingMessageProcessor(), 
            new ProcessIfStartedMessageProcessor(connector, connector.getLifecycleManager().getState()),
    
            // Everything is processed within TransactionTemplate
            new TransactionalInterceptingMessageProcessor(endpoint.getTransactionConfig(), connector.getExceptionListener()),
    
            new OutboundEventTimeoutMessageProcessor(),
    
            // Exception handling to preserve previous MuleSession level exception
            // handling behaviour
            new OutboundSimpleTryCatchMessageProcessor(),
            
            new OutboundSessionHandlerMessageProcessor(connector.getSessionHandler()),
            new OutboundEndpointPropertyMessageProcessor(),
    
            // TODO MULE-4872
            (endpoint instanceof OutboundEndpointDecorator ? 
                 new OutboundEndpointDecoratorMessageProcessor((OutboundEndpointDecorator) endpoint) : 
                 new ChainMessageProcessorBuilder.NullMessageProcessor()),
    
            new OutboundSecurityFilterMessageProcessor(endpoint),
            new OutboundTryCatchMessageProcessor(endpoint),
            new OutboundResponsePropertiesMessageProcessor(endpoint)
        };
    }
    
    /** Override this method to change the default MessageProcessors. */
    protected MessageProcessor[] createOutboundResponseMessageProcessors(OutboundEndpoint endpoint) throws MuleException
    {
        return new MessageProcessor[] 
        { 
            new TransformerMessageProcessor(endpoint.getResponseTransformers()),
            new OutboundRewriteResponseEventMessageProcessor()
        };
    }    
    
    public MessageProcessor createInboundMessageProcessorChain(InboundEndpoint endpoint, MessageProcessor target) throws MuleException
    {
        // -- REQUEST CHAIN --
        ChainMessageProcessorBuilder requestChainBuilder = new ChainMessageProcessorBuilder();
        requestChainBuilder.setName("Inbound endpoint request pipeline");
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
        ChainMessageProcessorBuilder responseChainBuilder = new ChainMessageProcessorBuilder();
        responseChainBuilder.setName("Inbound endpoint response pipeline");
        // Default MPs
        responseChainBuilder.chain(createInboundResponseMessageProcessors(endpoint));
        // Configured MPs (if any)
        responseChainBuilder.chain(endpoint.getResponseMessageProcessors());

        // Compose request and response chains. We do this so that if the request
        // chain returns early the response chain is still invoked.
        ChainMessageProcessorBuilder compositeChainBuilder = new ChainMessageProcessorBuilder();
        compositeChainBuilder.setName("Inbound endpoint request/response composite pipeline");
        compositeChainBuilder.chain(requestChainBuilder.build(), responseChainBuilder.build());
        return compositeChainBuilder.build();
    }

    public MessageProcessor createOutboundMessageProcessorChain(OutboundEndpoint endpoint, MessageProcessor target) throws MuleException
    {
        // -- REQUEST CHAIN --
        ChainMessageProcessorBuilder outboundChainBuilder = new ChainMessageProcessorBuilder();
        outboundChainBuilder.setName("Outbound endpoint request chain");
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
        ChainMessageProcessorBuilder responseChainBuilder = new ChainMessageProcessorBuilder();
        responseChainBuilder.setName("Outbound endpoint response chain");
        // Default MPs
        responseChainBuilder.chain(createOutboundResponseMessageProcessors(endpoint));
        // Configured MPs (if any)
        responseChainBuilder.chain(endpoint.getResponseMessageProcessors());

        // Compose request and response chains. We do this so that if the request
        // chain returns early the response chain is still invoked.
        ChainMessageProcessorBuilder compositeChainBuilder = new ChainMessageProcessorBuilder();
        compositeChainBuilder.setName("Outbound endpoint request/response composite chain");
        compositeChainBuilder.chain(outboundChainBuilder.build(), responseChainBuilder.build());
        return compositeChainBuilder.build();
    }
}


