/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.config.i18n.CoreMessages;
import org.mule.routing.outbound.AbstractMessageSequenceSplitter;
import org.mule.routing.outbound.ArrayMessageSequence;
import org.mule.routing.outbound.CollectionMessageSequence;
import org.mule.routing.outbound.IteratorMessageSequence;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Splits a message that has a Collection, Iterable, MessageSequence or Iterator
 * payload invoking the next message processor one
 * for each item in it.
 * <p>
 * <b>EIP Reference:</b> <a href="http://www.eaipatterns.com/Sequencer.html">http
 * ://www.eaipatterns.com/Sequencer.html</a>
 */
public class CollectionSplitter extends AbstractMessageSequenceSplitter
{
    @SuppressWarnings("unchecked")
    protected MessageSequence<?> splitMessageIntoSequence(MuleEvent event)
    {
        MuleMessage msg = event.getMessage();
        if (msg instanceof MuleMessageCollection)
        {
            return new ArrayMessageSequence(((MuleMessageCollection) msg).getMessagesAsArray());
        }
        Object payload = msg.getPayload();
        if (payload instanceof MessageSequence<?>)
        {
            return ((MessageSequence<?>) payload);
        }
        if (payload instanceof Iterator<?>)
        {
            return new IteratorMessageSequence<Object>(((Iterator<Object>) payload));
        }
        if (payload instanceof Collection)
        {
            return new CollectionMessageSequence(new LinkedList((Collection) payload));
        }
        if (payload instanceof Iterable<?>)
        {
            return new IteratorMessageSequence<Object>(((Iterable<Object>) payload).iterator());
        }
        if (payload instanceof Object[])
        {
            return new ArrayMessageSequence((Object[]) payload);
        }
        else
        {
            throw new IllegalArgumentException(CoreMessages.objectNotOfCorrectType(payload.getClass(),
                new Class[]{Iterable.class, Iterator.class, MessageSequence.class, Collection.class})
                .getMessage());
        }
    }
}
