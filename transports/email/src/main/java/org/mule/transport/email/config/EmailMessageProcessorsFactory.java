/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.email.config;

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

import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;

public class EmailMessageProcessorsFactory extends DefaultMessageProcessorsFactory
{
    @Override
    public MessageProcessor[] createInboundMessageProcessors(InboundEndpoint endpoint)
    {
        return new MessageProcessor[] 
        { 
            // This is identical to the list in DefaultMessageProcessorsFactory with the exception of EmailInboundFilterMessageProcessor
            new InboundEndpointPropertyMessageProcessor(endpoint),
            new InboundNotificationMessageProcessor(endpoint), 
            new InboundLoggingMessageProcessor(endpoint),
            new EmailInboundFilterMessageProcessor(), 
            new InboundSecurityFilterMessageProcessor(endpoint),
            new TransformerMessageProcessor(endpoint.getTransformers()) 
        };
    }
    
    class EmailInboundFilterMessageProcessor extends InboundFilterMessageProcessor
    {
        @Override
        protected MuleMessage handleUnacceptedFilter(MuleMessage message, InboundEndpoint endpoint)
        {
            super.handleUnacceptedFilter(message, endpoint);
            if (message.getPayload() instanceof Message)
            {
                Message msg = (Message) message.getPayload();
                try
                {
                    msg.setFlag(Flags.Flag.DELETED, endpoint.isDeleteUnacceptedMessages());
                }
                catch (MessagingException e)
                {
                    logger.error("failed to set message deleted: " + e.getMessage(), e);
                }
            }
            return message;
        }
    };
}
    