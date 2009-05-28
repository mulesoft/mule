/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.api.ThreadSafeAccess;
import org.mule.api.transformer.TransformerException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;

/**
 * A {@link org.mule.api.MuleMessage} type that manages a collection of MuleMessage Objects.
 * Typically this type of message is only used when users explicitly want to work with aggregated or re-sequenced
 * collections of messages.
 *
 *  Note that the {@link #getPayload()} for this message will return a {@link java.util.List} of payload objects for
 * each of the Mule messages stored in this collection.
 *
 * Calling {@link #getPayload(Class)} will attempt to transform all payloads and return a {@link java.util.List}.
 *
 * The methods {@link #getPayloadAsString()} and {@link #getPayloadAsBytes()} are unsupported, instead users should
 * call {@link #getPayload(Class)} and pass in the return type <code>byte[].class</code> or <code>String.class</code>.
 */
public class DefaultMessageCollection extends DefaultMuleMessage implements MuleMessageCollection
{
    private List messageList = new CopyOnWriteArrayList();

    public DefaultMessageCollection()
    {
        //This will be a collection of payloads
        super(new CopyOnWriteArrayList());
    }

    /**
     * Performs a shallow copy
     * @param msg
     */
    public DefaultMessageCollection(DefaultMessageCollection msg)
    {
        this();
        for (int i = 0; i < msg.getMessagesAsArray().length; i++)
        {
            addMessage(msg.getMessagesAsArray()[i]);
        }
    }

    public void addMessage(MuleMessage message)
    {
        getMessageList().add(message);
        getPayloadList().add(message.getPayload());
    }

    public MuleMessage[] getMessagesAsArray()
    {
        List list = getMessageList();
        MuleMessage[] messages = new MuleMessage[list.size()];
        messages = (MuleMessage[])list.toArray(messages);
        return messages;
    }

    public Object[] getPayloadsAsArray()
    {
        List list = getPayloadList();
        Object[] payloads = new Object[list.size()];
        payloads = list.toArray(payloads);
        return payloads;
    }

    public void removedMessage(MuleMessage message)
    {
        getMessageList().remove(message);
        getPayloadList().remove(message.getPayload());
    }

    public void addMessage(MuleMessage message, int index)
    {
        getMessageList().add(index, message);
        getPayloadList().add(index, message.getPayload());
    }

    public void addMessages(MuleEvent[] events)
    {
        for (int i = 0; i < events.length; i++)
        {
            MuleEvent event = events[i];
           addMessage(event.getMessage());
        }
    }

    public void addMessages(List messages)
    {
        for (Iterator iterator = messages.iterator(); iterator.hasNext();)
        {
            MuleMessage message = (MuleMessage) iterator.next();
            addMessage(message);
        }
    }

    public void addMessages(MuleMessage[] messages)
    {
        for (int i = 0; i < messages.length; i++)
        {
            addMessage(messages[i]);
        }
    }

    public MuleMessage getMessage(int index)
    {
        return (MuleMessage)getMessageList().get(index);
    }

    protected List getMessageList()
    {
        return messageList;
    }

    protected List getPayloadList()
    {
        return (List)getPayload();
    }

    /**
     * Applies the {@link #getPayload(Class)} call to every message in the collection and returns a
     * {@link java.util.List} of results.
     *
     * {@inheritDoc}
     */
    @Override
    public Object getPayload(Class outputType) throws TransformerException
    {
        List results = new ArrayList(getMessageList().size());
        for (Iterator iterator = getMessageList().iterator(); iterator.hasNext();)
        {
            MuleMessage message = (MuleMessage) iterator.next();
            results.add(message.getPayload(outputType));
        }
        return results;
    }

    public int size()
    {
        return getMessageList().size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] getPayloadAsBytes() throws Exception
    {
        throw new UnsupportedOperationException("getPayloadAsBytes(), use getPayload(byte[].class)");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPayloadAsString(String encoding) throws Exception
    {
        throw new UnsupportedOperationException("getPayloadAsString(), use getPayload(String[].class)");

    }

    /**
     * We need to overload this if we find we want to make this class available to users, but the copy will be expensive;
     */
    public ThreadSafeAccess newThreadCopy()
    {
        return new DefaultMessageCollection(this);
    }
}
