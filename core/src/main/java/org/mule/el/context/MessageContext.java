/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.context;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.transport.NullPayload;

import java.util.Map;

import javax.activation.DataHandler;

/**
 * Exposes information about the current Mule message:
 *
 * <li> <b>id</b>                    <i>The unique message id</i>
 * <li> <b>rootId</b>                <i>The root message id.  The id of the message before being split into parts.
 *                                      If was is not split then this value is the same as the id.</i>
 * <li> <b>correlationId</b>         <i>The message correlationId.</i>
 * <li> <b>correlationSequence</b>   <i>The message correlation sequence number.</i>
 * <li> <b>correlationGroupSize</b>  <i>The message correlation group size.</i>
 * <li> <b>dataType</b>              <i>The message data type (org.mule.api.transformer.DataType).</i>
 * <li> <b>replyTo</b>               <i>The message reply to destination. (mutable)</i>
 * <li> <b>payload</b>               <i>The message payload (mutable).  You can also use message.payloadAs(Class clazz).  Note: If the message payload is NullPayload, this method will return null (from 3.4)</i>
 * <li> <b>inboundProperties</b>     <i>Map of inbound message properties (immutable).</i>
 * <li> <b>outboundProperties</b>    <i>Map of outbound message properties.</i>
 * <li> <b>inboundAttachements</b>   <i>Map of inbound message attachments (immutable).</i>
 * <li> <b>outboundAttachements</b>  <i>Map of outbound message attachments.</i>
 */
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
        if (NullPayload.getInstance().equals(message.getPayload()))
        {
            // Return null for NullPayload because MEL user doesn't not know what NullPayload is and to allow
            // them to use null check (#[payload == null])
            return null;
        }
        else
        {
            return message.getPayload();
        }
    }

    public <T> T payloadAs(Class<T> type) throws TransformerException
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

    @Override
    public String toString()
    {
        return message.toString();
    }
}
