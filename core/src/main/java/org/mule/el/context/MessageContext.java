/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el.context;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;

import java.util.Map;

import javax.activation.DataHandler;

public class MessageContext
{
    protected MuleMessage message;

    public MessageContext(MuleMessage message)
    {
        this.message = message;
    }

    public String getId()
    {
        return message.getUniqueId();
    }

    public String getRootId()
    {
        return message.getMessageRootId();
    }

    public String getCorrelationId()
    {
        return message.getCorrelationId();
    }

    public int getCorrelationSequence()
    {
        return message.getCorrelationSequence();
    }

    public int getCorrelationGroupSize()
    {
        return message.getCorrelationGroupSize();
    }

    public Object getReplyTo()
    {
        return message.getReplyTo();
    }

    public void setReplyTo(String replyTo)
    {
        message.setReplyTo(replyTo);
    }

    public DataType<?> getDataType()
    {
        return message.getDataType();
    }

    public Object getPayload()
    {
        return message.getPayload();
    }

    public Object payloadAs(Class<?> type) throws TransformerException
    {
        return message.getPayload(type);
    }

    public Object payloadAs(DataType<?> dt) throws TransformerException
    {
        return message.getPayload(dt);
    }

    public void setPayload(Object payload)
    {
        message.setPayload(payload);
    }

    public Map<String, Object> getInboundProperties()
    {
        return new MessagePropertyMapContext(message, PropertyScope.INBOUND);
    }

    public Map<String, Object> getOutboundProperties()
    {
        return new MessagePropertyMapContext(message, PropertyScope.OUTBOUND);
    }

    public Map<String, DataHandler> getInboundAttachments()
    {
        return new InboundAttachmentMapContext(message);
    }

    public Map<String, DataHandler> getOutboundAttachments()
    {
        return new OutboundAttachmentMapContext(message);
    }

}
