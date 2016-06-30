/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.transport;

import org.mule.compatibility.core.api.transport.MessageTypeNotSupportedException;
import org.mule.compatibility.core.api.transport.MuleMessageFactory;
import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MutableMuleMessage;

import java.nio.charset.Charset;

public abstract class AbstractMuleMessageFactory implements MuleMessageFactory
{
    protected MuleContext muleContext;

    /**
     * Required by subclasses to instantiate factory through reflection.
     */
    public AbstractMuleMessageFactory()
    {
    }

    @Override
    public MutableMuleMessage create(Object transportMessage, MuleMessage previousMessage, Charset encoding) throws Exception
    {
        return doCreate(transportMessage, previousMessage, encoding);
    }

    @Override
    public MutableMuleMessage create(Object transportMessage, Charset encoding) throws Exception
    {
        return doCreate(transportMessage, null, encoding);
    }

    private MutableMuleMessage doCreate(Object transportMessage, MuleMessage previousMessage, Charset encoding)
            throws Exception
    {
        if (transportMessage == null)
        {
            return new DefaultMuleMessage(NullPayload.getInstance());
        }

        if (!isTransportMessageTypeSupported(transportMessage))
        {
            throw new MessageTypeNotSupportedException(transportMessage, getClass());
        }

        Object payload = extractPayload(transportMessage, encoding);
        final DataType dataType = DataType.builder().type((Class) (payload == null ? Object.class : payload.getClass())).mediaType(getMimeType(transportMessage)).charset(encoding).build();
        MutableMuleMessage message;
        if (previousMessage != null)
        {
            message = new DefaultMuleMessage(payload, previousMessage, dataType);
        }
        else
        {
            message = new DefaultMuleMessage(payload, dataType);
        }

        message = addProperties(message, transportMessage);
        message = addAttachments(message, transportMessage);
        return message;
    }

    protected String getMimeType(Object transportMessage)
    {
        return null;
    }

    protected abstract Class<?>[] getSupportedTransportMessageTypes();

    protected abstract Object extractPayload(Object transportMessage, Charset encoding) throws Exception;

    protected MutableMuleMessage addProperties(MutableMuleMessage message, Object transportMessage) throws Exception
    {
        // Template method
        return message;
    }

    protected MutableMuleMessage addAttachments(MutableMuleMessage message, Object transportMessage) throws Exception
    {
        // Template method
        return message;
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
