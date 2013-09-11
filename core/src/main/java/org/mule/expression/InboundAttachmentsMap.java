/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import org.mule.api.MuleMessage;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;

/**
 * Creates a wrapper around Mule Message with a MAp facade used for allowing developers to add attachments to
 * an outgoing message in a transformer of component without needing to access the Mule API directly
 */
public class InboundAttachmentsMap implements Map<String, DataHandler>
{
    private MuleMessage message;

    public InboundAttachmentsMap(MuleMessage message)
    {
        this.message = message;
    }

    public int size()
    {
        return message.getInboundAttachmentNames().size();
    }

    public boolean isEmpty()
    {
        return message.getInboundAttachmentNames().size() == 0;
    }

    public boolean containsKey(Object key)
    {
        return message.getInboundAttachmentNames().contains(key.toString());
    }

    public boolean containsValue(Object value)
    {
        return values().contains(value);
    }

    public DataHandler get(Object key)
    {
        return message.getInboundAttachment(key.toString());
    }

    public DataHandler put(String key, DataHandler value)
    {
        throw new UnsupportedOperationException("put(): Inbound attachments are read-only");
    }


    public DataHandler remove(Object key)
    {
        throw new UnsupportedOperationException("remove(): Inbound attachments are read-only");
    }


    public void putAll(Map<? extends String, ? extends DataHandler> map)
    {
        for (Entry<? extends String, ? extends DataHandler> entry : map.entrySet())
        {
            put(entry.getKey(), entry.getValue());
        }
    }

    public void clear()
    {
        throw new UnsupportedOperationException("clear");
    }

    public Set<String> keySet()
    {
        return message.getInboundAttachmentNames();
    }

    public Collection<DataHandler> values()
    {
        return getAttachments().values();
    }

    public Set<Entry<String, DataHandler>> entrySet()
    {
        return getAttachments().entrySet();
    }

    //TODO Could optimise this to cache if no writes are made
    private Map<String, DataHandler> getAttachments()
    {
        Map<String, DataHandler> props = new HashMap<String, DataHandler>();
        for (String s : message.getInboundAttachmentNames())
        {
            props.put(s, message.getInboundAttachment(s));
        }
        return props;
    }
}
