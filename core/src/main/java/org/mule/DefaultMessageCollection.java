/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.api.ThreadSafeAccess;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.types.DataTypeFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A {@link org.mule.api.MuleMessage} type that manages a collection of MuleMessage Objects.
 * Typically this type of message is only used when users explicitly want to work with aggregated or re-sequenced
 * collections of messages.
 *
 *  Note that the {@link #getPayload()} for this message will return a {@link java.util.List} of payload objects for
 * each of the Mule messages stored in this collection.
 *
 * Calling {@link org.mule.api.MuleMessage#getPayload(Class)} will attempt to transform all payloads and return a {@link java.util.List}.
 *
 * The methods {@link org.mule.api.MuleMessage#getPayloadAsString()} and {@link org.mule.api.MuleMessage#getPayloadAsBytes()} are unsupported, instead users should
 * call {@link org.mule.api.MuleMessage#getPayload(Class)} and pass in the return type <code>byte[].class</code> or <code>String.class</code>.
 */
public class DefaultMessageCollection extends DefaultMuleMessage implements MuleMessageCollection
{
    private List messageList = new CopyOnWriteArrayList();

    private boolean invalidatedPayload;

    public DefaultMessageCollection(MuleContext muleContext)
    {
        //This will be a collection of payloads
        super(new CopyOnWriteArrayList(), muleContext);
        invalidatedPayload = false;
    }

    /**
     * Performs a shallow copy
     * @param msg
     * @param muleContext
     */
    public DefaultMessageCollection(DefaultMessageCollection msg, MuleContext muleContext)
    {
        this(msg, muleContext, false);
    }

    /**
     * Performs a shallow or deep copy of the messages
     * @param msg
     * @param muleContext
     * @param deepMessageCopy
     */
    public DefaultMessageCollection(DefaultMessageCollection msg, MuleContext muleContext, boolean deepMessageCopy)
    {
        this(muleContext);
        setUniqueId(msg.getUniqueId());
        setMessageRootId(msg.getMessageRootId());
        copyMessageProperties(msg);

        if (!msg.invalidatedPayload)
        {
            MuleMessage[] messagesAsArray = msg.getMessagesAsArray();
            for (int i = 0; i < messagesAsArray.length; i++)
            {
                MuleMessage currentMsg = messagesAsArray[i];
                if (deepMessageCopy)
                {
                    if (currentMsg instanceof MuleMessageCollection)
                    {
                        addMessage(new DefaultMessageCollection((DefaultMessageCollection) currentMsg, muleContext, true));
                    }
                    else
                    {
                        addMessage(new DefaultMuleMessage(currentMsg, currentMsg, muleContext));
                    }
                }
                else
                {
                    addMessage(currentMsg);
                }
            }
        }
        else
        {
            invalidatedPayload = true;
        }
    }

    protected void checkValidPayload()
    {
        if (invalidatedPayload)
        {
            throw new IllegalStateException("Payload was invalidated calling setPayload and the message is not collection anymore.");
        }
    }

    public void addMessage(MuleMessage message)
    {
        checkValidPayload();
        getMessageList().add(message);
        getPayloadList().add(message.getPayload());
    }

    public MuleMessage[] getMessagesAsArray()
    {
        checkValidPayload();
        List list = getMessageList();
        MuleMessage[] messages = new MuleMessage[list.size()];
        messages = (MuleMessage[])list.toArray(messages);
        return messages;
    }

    public Object[] getPayloadsAsArray()
    {
        checkValidPayload();
        List list = getPayloadList();
        Object[] payloads = new Object[list.size()];
        payloads = list.toArray(payloads);
        return payloads;
    }

    public void removedMessage(MuleMessage message)
    {
        checkValidPayload();
        getMessageList().remove(message);
        getPayloadList().remove(message.getPayload());
    }

    public void addMessage(MuleMessage message, int index)
    {
        checkValidPayload();
        getMessageList().add(index, message);
        getPayloadList().add(index, message.getPayload());
    }

    public void addMessages(MuleEvent[] events)
    {
        checkValidPayload();
        for (int i = 0; i < events.length; i++)
        {
            MuleEvent event = events[i];
            addMessage(event.getMessage());
        }
    }

    public void addMessages(List messages)
    {
        checkValidPayload();
        for (Iterator iterator = messages.iterator(); iterator.hasNext(); )
        {
            MuleMessage message = (MuleMessage) iterator.next();
            addMessage(message);
        }
    }

    public void addMessages(MuleMessage[] messages)
    {
        checkValidPayload();
        for (int i = 0; i < messages.length; i++)
        {
            addMessage(messages[i]);
        }
    }

    public MuleMessage getMessage(int index)
    {
        checkValidPayload();
        return (MuleMessage) getMessageList().get(index);
    }

    protected List getMessageList()
    {
        checkValidPayload();
        return messageList;
    }

    protected List getPayloadList()
    {
        checkValidPayload();
        return (List) getPayload();
    }

    @Override
    public synchronized void setPayload(Object payload)
    {
        if (this.getPayload() == payload)
        {
            return;
        }
        else
        {
            super.setPayload(payload);
            invalidatedPayload = true;
        }
    }

    /**
     * Applies the {@link org.mule.api.MuleMessage#getPayload(Class)} call to every message in the collection and returns a
     * {@link java.util.List} of results.
     *
     * {@inheritDoc}
     */
    @Override
    public Object getPayload(Class outputType) throws TransformerException
    {
        if (invalidatedPayload)
        {
            return super.getPayload(outputType);
        }
        else
        {
            DataType outputDataType = DataTypeFactory.create(outputType);
            List results = new ArrayList(getMessageList().size());
            for (Iterator iterator = getMessageList().iterator(); iterator.hasNext(); )
            {
                MuleMessage message = (MuleMessage) iterator.next();
                results.add(message.getPayload(outputDataType));
            }
            return results;
        }
    }

    public int size()
    {
        checkValidPayload();
        return getMessageList().size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] getPayloadAsBytes() throws Exception
    {
        if (invalidatedPayload)
        {
            return super.getPayloadAsBytes();
        }
        else
        {
            throw new UnsupportedOperationException("getPayloadAsBytes(), use getPayload(DataType.BYTE_ARRAY_DATA_TYPE)");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPayloadAsString(String encoding) throws Exception
    {
        if (invalidatedPayload)
        {
            return super.getPayloadAsString(encoding);
        }
        else
        {
            throw new UnsupportedOperationException("getPayloadAsString(), use getPayload(DataType.STRING_DATA_TYPE)");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPayloadForLogging(String encoding)
    {
        if (invalidatedPayload)
        {
            return super.getPayloadForLogging(encoding);
        }
        else
        {
            return "[This is a message collection]";
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPayloadForLogging()
    {
        if (invalidatedPayload)
        {
            return super.getPayloadForLogging();
        }
        else
        {
            return "[This is a message collection]";
        }
    }

    /**
     * We need to overload this if we find we want to make this class available to users, but the copy will be expensive;
     */
    public ThreadSafeAccess newThreadCopy()
    {
        if (invalidatedPayload)
        {
            return super.newThreadCopy();
        }
        return new DefaultMessageCollection(this, muleContext, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MuleMessage createInboundMessage() throws Exception
    {
        if (invalidatedPayload)
        {
            return super.createInboundMessage();
        }
        else
        {
            DefaultMessageCollection newMessage = new DefaultMessageCollection(getMuleContext());
            newMessage.setUniqueId(getUniqueId());
            newMessage.setMessageRootId(getMessageRootId());
            MuleMessage[] messages = getMessagesAsArray();
            for (MuleMessage message : messages)
            {
                newMessage.addMessage(message.createInboundMessage());
            }
            copyToInbound(newMessage);
            return newMessage;
        }
    }

    public boolean isInvalidatedPayload()
    {
        return invalidatedPayload;
    }
}
