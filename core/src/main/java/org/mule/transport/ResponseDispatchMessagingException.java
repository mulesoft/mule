/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.Message;

/**
 *
 */
class ResponseDispatchMessagingException extends MessagingException
{
    ResponseDispatchMessagingException(Message message, MuleEvent event)
    {
        super(message, event);
    }

    ResponseDispatchMessagingException(Message message, MuleEvent event, MessageProcessor failingMessageProcessor)
    {
        super(message, event, failingMessageProcessor);
    }

    ResponseDispatchMessagingException(Message message, MuleEvent event, Throwable cause)
    {
        super(message, event, cause);
    }

    ResponseDispatchMessagingException(Message message, MuleEvent event, Throwable cause, MessageProcessor failingMessageProcessor)
    {
        super(message, event, cause, failingMessageProcessor);
    }

    ResponseDispatchMessagingException(MuleEvent event, Throwable cause)
    {
        super(event, cause);
    }

    ResponseDispatchMessagingException(MuleEvent event, Throwable cause, MessageProcessor failingMessageProcessor)
    {
        super(event, cause, failingMessageProcessor);
    }
}
