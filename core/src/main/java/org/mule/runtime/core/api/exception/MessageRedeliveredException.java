/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.exception;

import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.endpoint.ImmutableEndpoint;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.config.i18n.Message;

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

    /**
     * @deprecated Transport infrastructure is deprecated.
     */
    @Deprecated
    protected MessageRedeliveredException(String messageId, int redeliveryCount, int maxRedelivery, InboundEndpoint endpoint, MuleEvent event, Message message)
    {
        super(message, event);
        this.messageId = messageId;
        this.redeliveryCount = redeliveryCount;
        this.maxRedelivery = maxRedelivery;
        this.endpoint = endpoint;
    }

    /**
     * @deprecated Transport infrastructure is deprecated.
     */
    @Deprecated
    public MessageRedeliveredException(String messageId, int redeliveryCount, int maxRedelivery, InboundEndpoint endpoint, MuleEvent event, Message message, MessageProcessor failingMessageProcessor)
    {
        super(message, event, failingMessageProcessor);
        this.messageId = messageId;
        this.redeliveryCount = redeliveryCount;
        this.maxRedelivery = maxRedelivery;
        this.endpoint = endpoint;
    }

    public MessageRedeliveredException(String messageId, int redeliveryCount, int maxRedelivery, MuleEvent event, Message message, MessageProcessor failingMessageProcessor)
    {
        this(messageId, redeliveryCount, maxRedelivery, null, event, message, failingMessageProcessor);
    }

    /**
     * @deprecated Transport infrastructure is deprecated.
     */
    @Deprecated
    public MessageRedeliveredException(String messageId, int redeliveryCount, int maxRedelivery, InboundEndpoint endpoint, MuleEvent event, MessageProcessor failingMessageProcessor)
    {
        this(messageId, redeliveryCount, maxRedelivery, endpoint, event, CoreMessages.createStaticMessage("Maximum redelivery attempts reached"), failingMessageProcessor);
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

    /**
     * @deprecated Transport infrastructure is deprecated.
     */
    @Deprecated
    public ImmutableEndpoint getEndpoint()
    {
        return endpoint;
    }
}
