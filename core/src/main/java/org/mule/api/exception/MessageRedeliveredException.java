/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.exception;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;

public class MessageRedeliveredException extends MessagingException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 9013890402770563931L;

    protected final transient ImmutableEndpoint endpoint;
    String messageId;
    int redeliveryCount;
    int maxRedelivery;

    public MessageRedeliveredException(String messageId, int redeliveryCount, int maxRedelivery, InboundEndpoint endpoint, MuleEvent event, Message message)
    {
        super(message,event);
        this.messageId = messageId;
        this.redeliveryCount = redeliveryCount;
        this.maxRedelivery = maxRedelivery;
        this.endpoint = endpoint;
    }

    public MessageRedeliveredException(String messageId, int redeliveryCount, int maxRedelivery, InboundEndpoint endpoint, MuleEvent event)
    {
        this(messageId, redeliveryCount, maxRedelivery, endpoint, event, CoreMessages.createStaticMessage("Maximum redelivery attempts reached"));
    }

    public String getMessageId()
    {
        return messageId;
    }

    public int getRedeliveryCount()
    {
        return redeliveryCount;
    }

    public int getMaxRedelivery()
    {
        return maxRedelivery;
    }

    public ImmutableEndpoint getEndpoint()
    {
        return endpoint;
    }
}
