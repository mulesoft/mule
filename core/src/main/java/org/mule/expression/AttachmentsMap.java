/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
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
 *
 */
//TODO add a helper method for creating the DataHandler from an object
public class AttachmentsMap implements Map<String, DataHandler>
{
    private MuleMessage message;

    public AttachmentsMap(MuleMessage message)
    {
        this.message = message;
    }

    public int size()
    {
        return message.getAttachmentNames().size();
    }

    public boolean isEmpty()
    {
        return message.getAttachmentNames().size() == 0;
    }

    public boolean containsKey(Object key)
    {
        return message.getAttachmentNames().contains(key.toString());
    }

    public boolean containsValue(Object value)
    {
        return values().contains(value);
    }

    public DataHandler get(Object key)
    {
        return message.getAttachment(key.toString());
    }

    public DataHandler put(String key, DataHandler value)
    {
        try
        {
            message.addAttachment(key, value);
            return value;
        }
        catch (Exception e)
        {
            //Not sure we can do anything else here
            throw new RuntimeException(e);
        }
    }


    public DataHandler remove(Object key)
    {
        DataHandler attachment = message.getAttachment(key.toString());
        try
        {
            message.removeAttachment(key.toString());
        }
        catch (Exception e)
        {
            //ignore (some message types may throw an exception if the attachment does not exist)
        }
        return attachment;
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
        return message.getAttachmentNames();
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
        for (String s : message.getAttachmentNames())
        {
            props.put(s, message.getAttachment(s));
        }
        return props;
    }
}
