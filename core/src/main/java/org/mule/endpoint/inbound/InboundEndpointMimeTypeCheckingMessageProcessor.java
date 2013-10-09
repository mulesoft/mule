/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
