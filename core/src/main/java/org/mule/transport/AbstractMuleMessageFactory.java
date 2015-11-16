/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.transport.MessageTypeNotSupportedException;
import org.mule.api.transport.MuleMessageFactory;

public abstract class AbstractMuleMessageFactory implements MuleMessageFactory
{
    protected MuleContext muleContext;

    /**
     * Required by subclasses to instantiate factory through reflection.
     */
    public AbstractMuleMessageFactory()
    {
    }

    public MuleMessage create(Object transportMessage, MuleMessage previousMessage, String encoding, MuleContext muleContext) throws Exception
    {
        return doCreate(transportMessage, previousMessage, encoding, muleContext);
    }

    @Override
    public MuleMessage create(Object transportMessage, String encoding, MuleContext muleContext) throws Exception
    {
        return doCreate(transportMessage, null, encoding, muleContext);
    }

    private MuleMessage doCreate(Object transportMessage, MuleMessage previousMessage, String encoding, MuleContext muleContext)
            throws Exception
    {
        if (transportMessage == null)
        {
            return new DefaultMuleMessage(NullPayload.getInstance(), muleContext);
        }

        if (!isTransportMessageTypeSupported(transportMessage))
        {
            throw new MessageTypeNotSupportedException(transportMessage, getClass());
        }

        Object payload = extractPayload(transportMessage, encoding);
        DefaultMuleMessage message;
        if (previousMessage != null)
        {
            message = new DefaultMuleMessage(payload, previousMessage, muleContext);
        }
        else
        {
            message = new DefaultMuleMessage(payload, muleContext);
        }

        message.setEncoding(encoding);
        message.setMimeType(getMimeType(transportMessage));
        addProperties(message, transportMessage);
        addAttachments(message, transportMessage);
        return message;
    }

    protected String getMimeType(Object transportMessage)
    {
        return null;
    }

    protected abstract Class<?>[] getSupportedTransportMessageTypes();

    protected abstract Object extractPayload(Object transportMessage, String encoding) throws Exception;

    protected void addProperties(DefaultMuleMessage message, Object transportMessage) throws Exception
    {
        // Template method
    }

    protected void addAttachments(DefaultMuleMessage message, Object transportMessage) throws Exception
    {
        // Template method
    }

    private boolean isTransportMessageTypeSupported(Object transportMessage)
    {
        Class<?> transportMessageType = transportMessage.getClass();
        boolean match = false;
        for (Class<?> type : getSupportedTransportMessageTypes())
        {
            if (type.isAssignableFrom(transportMessageType))
            {
                match = true;
                break;
            }
        }
        return match;
    }
}
