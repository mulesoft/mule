/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.context;

import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.MuleMessageCorrelation;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.transformer.TransformerException;

import java.io.Serializable;
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
 * <li> <b>dataType</b>              <i>The message data type (org.mule.runtime.core.api.transformer.DataType).</i>
 * <li> <b>replyTo</b>               <i>The message reply to destination. (mutable)</i>
 * <li> <b>payload</b>               <i>The message payload (mutable).  You can also use message.payloadAs(Class clazz).  Note: If the message payload is NullPayload, this method will return null (from 3.4)</i>
 * <li> <b>inboundProperties</b>     <i>Map of inbound message properties (immutable).</i>
 * <li> <b>outboundProperties</b>    <i>Map of outbound message properties.</i>
 * <li> <b>inboundAttachements</b>   <i>Map of inbound message attachments (immutable).</i>
 * <li> <b>outboundAttachements</b>  <i>Map of outbound message attachments.</i>
 */
public class MessageContext
{
    private MuleEvent event;
    private MuleContext muleContext;

    public MessageContext(MuleEvent event, MuleContext muleContext)
    {
        this.event = event;
        this.muleContext = muleContext;
    }

    public String getId()
    {
        return event.getMessage().getUniqueId();
    }

    public String getRootId()
    {
        return event.getMessage().getMessageRootId();
    }

    public MuleMessageCorrelation getCorrelation()
    {
        return event.getMessage().getCorrelation();
    }

    public Object getReplyTo()
    {
        return event.getMessage().getReplyTo();
    }

    public void setReplyTo(String replyTo)
    {
        event.setMessage(MuleMessage.builder(event.getMessage()).replyTo(replyTo).build());
    }

    public DataType getDataType()
    {
        return event.getMessage().getDataType();
    }

    public Object getPayload()
    {
        if (NullPayload.getInstance().equals(event.getMessage().getPayload()))
        {
            // Return null for NullPayload because MEL user doesn't not know what NullPayload is and to allow
            // them to use null check (#[payload == null])
            return null;
        }
        else
        {
            return event.getMessage().getPayload();
        }
    }

    /**
     * Obtains the payload of the current message transformed to the given #type.
     *
     * @param type the java type the payload is to be transformed to
     * @return the transformed payload
     * @throws TransformerException
     */
    public <T> T payloadAs(Class<T> type) throws TransformerException
    {
        event.setMessage(muleContext.getTransformationService().transform(event.getMessage(), DataType.fromType(type)));
        return (T) event.getMessage().getPayload();
    }

    /**
     * Obtains the payload of the current message transformed to the given #dataType.
     *
     * @param dataType the DatType to transform the current message payload to
     * @return the transformed payload
     * @throws TransformerException if there is an error during transformation
     */
    public Object payloadAs(DataType dataType) throws TransformerException
    {
        event.setMessage(muleContext.getTransformationService().transform(event.getMessage(), dataType));
        return event.getMessage().getPayload();
    }

    public void setPayload(Object payload)
    {
        event.setMessage(MuleMessage.builder(event.getMessage()).payload(payload).build());
    }

    public Map<String, Serializable> getInboundProperties()
    {
        return new InboundPropertiesMapContext(event);
    }

    public Map<String, Serializable> getOutboundProperties()
    {
        return new OutboundPropertiesMapContext(event);
    }

    public Map<String, DataHandler> getInboundAttachments()
    {
        return new InboundAttachmentMapContext(event);
    }

    public Map<String, DataHandler> getOutboundAttachments()
    {
        return new OutboundAttachmentMapContext(event);
    }

    public Attributes getAttributes()
    {
        return event.getMessage().getAttributes();
    }

    @Override
    public String toString()
    {
        return event.getMessage().toString();
    }
}
