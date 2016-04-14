/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.exception;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.Message;

public class MessageRedeliveredException extends MessagingException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 9013890402770563931L;

    String messageId;
    int redeliveryCount;
    int maxRedelivery;

    public MessageRedeliveredException(String messageId, int redeliveryCount, int maxRedelivery, MuleEvent event, Message message, MessageProcessor failingMessageProcessor)
    {
        super(message, event, failingMessageProcessor);
        this.messageId = messageId;
        this.redeliveryCount = redeliveryCount;
        this.maxRedelivery = maxRedelivery;
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

}
