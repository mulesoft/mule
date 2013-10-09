/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.transformer;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.config.i18n.Message;

/**
 * An exception that occurred while transforming a message.
 */
public class TransformerMessagingException extends MessagingException
{
    private transient Transformer transformer;

    public TransformerMessagingException(Message message, MuleEvent event, Transformer transformer)
    {
        super(message, event);
        this.transformer = transformer;
    }

    public TransformerMessagingException(Message message, MuleEvent event, Transformer transformer, Throwable cause)
    {
        super(message, event, cause);
        this.transformer = transformer;
    }

    public TransformerMessagingException(MuleEvent event, Transformer transformer, Throwable cause)
    {
        super(event, cause);
        this.transformer = transformer;
    }

    public Transformer getTransformer()
    {
        return transformer;
    }
}
