/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.endpoint.inbound;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.ObjectUtils;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

/**
 * Verify that the inbound mime type is acceptable by this endpoint.
 */
public class InboundEndpointMimeTypeCheckingMessageProcessor implements MessageProcessor
{
    private InboundEndpoint endpoint;

    public InboundEndpointMimeTypeCheckingMessageProcessor(InboundEndpoint endpoint)
    {
        this.endpoint = endpoint;
    }

    public MuleEvent process(MuleEvent event) throws MessagingException
    {
        String endpointMimeType = endpoint.getMimeType();
        if (endpointMimeType != null)
        {
            MuleMessage message = event.getMessage();
            String contentType = message.getProperty(MuleProperties.CONTENT_TYPE_PROPERTY, PropertyScope.INBOUND);
            if (contentType == null)
            {
                contentType = message.getProperty(MuleProperties.CONTENT_TYPE_PROPERTY, PropertyScope.OUTBOUND);
            }
            if (contentType == null)
            {
                message.setProperty(MuleProperties.CONTENT_TYPE_PROPERTY, endpointMimeType, PropertyScope.INBOUND);
            }
            else
            {
                try
                {
                    MimeType mt = new MimeType(contentType);
                    String messageMimeType = mt.getPrimaryType() + "/" + mt.getSubType();
                    if (!messageMimeType.equals(endpointMimeType))
                    {
                        throw new MessagingException(
                            CoreMessages.unexpectedMIMEType(messageMimeType, endpointMimeType), event, this);
                    }
                }
                catch (MimeTypeParseException ex)
                {
                    throw new MessagingException(CoreMessages.illegalMIMEType(contentType), event, ex, this);
                }
            }
        }

        return event;
    }

    @Override
    public String toString()
    {
        return ObjectUtils.toString(this);
    }
}
