/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.processor;

import org.mule.api.MuleException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.endpoint.OutboundEndpointDecorator;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorsFactory;
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
import org.mule.processor.builder.ChainMessageProcessorBuilder;
import org.mule.transformer.TransformerMessageProcessor;
import org.mule.transport.AbstractConnector;

public class DefaultMessageProcessorsFactory implements MessageProcessorsFactory
{
    /** Override this method to change the default MessageProcessors. */
    public MessageProcessor[] createInboundMessageProcessors(InboundEndpoint endpoint)
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
    public MessageProcessor[] createInboundResponseMessageProcessors(InboundEndpoint endpoint)
    {
        return new MessageProcessor[] 
        { 
            new InboundExceptionDetailsMessageProcessor(endpoint.getConnector()),
            new TransformerMessageProcessor(endpoint.getResponseTransformers()) 
        };
    }
    
    /** Override this method to change the default MessageProcessors. */
    public MessageProcessor[] createOutboundMessageProcessors(OutboundEndpoint endpoint) throws MuleException
    {
        // TODO Need to clean this up the class hierarchy, but I'm not sure we want to put all these things in the Connector interface.
        AbstractConnector connector = ((AbstractConnector) endpoint.getConnector());
        
        return new MessageProcessor[] 
        { 
            // Log but don't proceed if connector is not started
            new OutboundLoggingMessageProcessor(), 
            connector.createAssertConnectorStartedMessageProcessor(),
    
            // Everything is processed within TransactionTemplate
            new TransactionalInterceptingMessageProcessor(endpoint.getTransactionConfig(), connector.getExceptionListener()),
    
            new OutboundEventTimeoutMessageProcessor(),
    
            // Exception handling to preserve previous MuleSession level exception
            // handling behaviour
            new OutboundSimpleTryCatchMessageProcessor(),
            connector.createAsyncInterceptingMessageProcessor(),
            
            new OutboundSessionHandlerMessageProcessor(connector.getSessionHandler()),
            new OutboundEndpointPropertyMessageProcessor(),
    
            // TODO MULE-4872
            (endpoint instanceof OutboundEndpointDecorator ? 
                 new OutboundEndpointDecoratorMessageProcessor((OutboundEndpointDecorator) endpoint) : 
                 new ChainMessageProcessorBuilder.NullMessageProcesser()),
    
            new OutboundSecurityFilterMessageProcessor(endpoint),
            new OutboundTryCatchMessageProcessor(endpoint),
            new OutboundResponsePropertiesMessageProcessor(endpoint)
        };
    }
    
    /** Override this method to change the default MessageProcessors. */
    public MessageProcessor[] createOutboundResponseMessageProcessors(OutboundEndpoint endpoint) throws MuleException
    {
        return new MessageProcessor[] 
        { 
            new TransformerMessageProcessor(endpoint.getResponseTransformers()),
            new OutboundRewriteResponseEventMessageProcessor()
        };
    }    
}


