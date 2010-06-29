/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.config;

import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.endpoint.inbound.InboundEndpointPropertyMessageProcessor;
import org.mule.endpoint.inbound.InboundFilterMessageProcessor;
import org.mule.endpoint.inbound.InboundLoggingMessageProcessor;
import org.mule.endpoint.inbound.InboundNotificationMessageProcessor;
import org.mule.endpoint.inbound.InboundSecurityFilterMessageProcessor;
import org.mule.processor.DefaultMessageProcessorsFactory;
import org.mule.transformer.TransformerMessageProcessor;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

public class HttpMessageProcessorsFactory extends DefaultMessageProcessorsFactory
{
    @Override
    public MessageProcessor[] createInboundMessageProcessors(InboundEndpoint endpoint)
    {
        return new MessageProcessor[] 
        { 
            // This is identical to the list in DefaultMessageProcessorsFactory with the exception of HttpInboundFilterMessageProcessor
            new InboundEndpointPropertyMessageProcessor(endpoint),
            new InboundNotificationMessageProcessor(endpoint), 
            new InboundLoggingMessageProcessor(endpoint),
            new HttpInboundFilterMessageProcessor(), 
            new InboundSecurityFilterMessageProcessor(endpoint),
            new TransformerMessageProcessor(endpoint.getTransformers()) 
        };
    }
    
    class HttpInboundFilterMessageProcessor extends InboundFilterMessageProcessor
    {
        @Override
        protected MuleMessage handleUnacceptedFilter(MuleMessage message, InboundEndpoint endpoint)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Message request '"
                             + message.getProperty(HttpConnector.HTTP_REQUEST_PROPERTY)
                             + "' is being rejected since it does not match the filter on this endpoint: "
                             + endpoint);
            }
            message.setProperty(HttpConnector.HTTP_STATUS_PROPERTY,
                String.valueOf(HttpConstants.SC_NOT_ACCEPTABLE));
            return message;
        }
    };
}


