/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.gs;

import java.util.Map;

import net.jini.core.entry.Entry;

import org.mule.umo.UMOExceptionPayload;
import org.mule.util.MapUtils;

/**
 * The default wrapper Template for a GigaSpace entry
 */
public class JiniMessage implements Entry
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 2373851288156547780L;

    public Object destination;
    public Object payload;
    public String correlationId;
    public Integer correlationSequence;
    public Integer correlationGroupSize;
    public Object replyTo;
    public String messageId;
    public Map properties;
    public String encoding;
    public UMOExceptionPayload exceptionPayload;
    public String payloadType;

    public JiniMessage()
    {
        super();
    }

    /**
     * @param destination
     * @param payload
     */
    public JiniMessage(Object destination, Object payload)
    {
        super();
        this.destination = destination;
        setPayload(payload);
    }

    public String getCorrelationId()
    {
        return correlationId;
    }

    public void setCorrelationId(String correlationId)
    {
        this.correlationId = correlationId;
    }

    public Integer getCorrelationSequence()
    {
        return correlationSequence;
    }

    public void setCorrelationSequence(Integer correlationSequence)
    {
        this.correlationSequence = correlationSequence;
    }

    public Integer getCorrelationGroupSize()
    {
        return correlationGroupSize;
    }

    public void setCorrelationGroupSize(Integer correlationGroupSize)
    {
        this.correlationGroupSize = correlationGroupSize;
    }

    public Object getReplyTo()
    {
        return replyTo;
    }

    public void setReplyTo(Object replyTo)
    {
        this.replyTo = replyTo;
    }

    public String getMessageId()
    {
        return messageId;
    }

    public void setMessageId(String messageId)
    {
        this.messageId = messageId;
    }

    /**
     * @return Returns the destination.
     */
    public Object getDestination()
    {
        return destination;
    }

    /**
     * @param destination The destination to set.
     */
    public void setDestination(Object destination)
    {
        this.destination = destination;
    }

    /**
     * @return Returns the payload.
     */
    public Object getPayload()
    {
        return payload;
    }

    /**
     * @param payload The payload to set.
     */
    public void setPayload(Object payload)
    {
        this.payload = payload;
        if (payload != null)
        {
            payloadType = payload.getClass().getName();
        }
        else
        {
            payloadType = null;
        }
    }

    public Map getProperties()
    {
        return properties;
    }

    public void setProperties(Map properties)
    {
        this.properties = properties;
    }

    public String getEncoding()
    {
        return encoding;
    }

    public void setEncoding(String encoding)
    {
        this.encoding = encoding;
    }

    public UMOExceptionPayload getExceptionPayload()
    {
        return exceptionPayload;
    }

    public void setExceptionPayload(UMOExceptionPayload exceptionPayload)
    {
        this.exceptionPayload = exceptionPayload;
    }

    public String getPayloadType()
    {
        return payloadType;
    }

    public void setPayloadType(String payloadType)
    {
        this.payloadType = payloadType;
    }

    public String toString()
    {
        StringBuffer buf = new StringBuffer(120);
        buf.append(getClass().getName());
        buf.append('{');
        buf.append("id=").append(getMessageId());
        buf.append(", payload=").append((getPayload() != null ? getPayload().getClass().getName() : "null"));
        buf.append(", correlationId=").append(getCorrelationId());
        buf.append(", correlationGroup=").append(getCorrelationGroupSize());
        buf.append(", correlationSeq=").append(getCorrelationSequence());
        buf.append(", encoding=").append(getEncoding());
        buf.append(", exceptionPayload=").append(getExceptionPayload());
        buf.append(", properties=").append(MapUtils.toString(properties, true));
        buf.append('}');
        return buf.toString();
    }

}
