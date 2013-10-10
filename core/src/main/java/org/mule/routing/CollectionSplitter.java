/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.mule.api.MuleEvent;
import org.mule.config.i18n.CoreMessages;
import org.mule.routing.outbound.AbstractMessageSequenceSplitter;
import org.mule.routing.outbound.CollectionMessageSequence;
import org.mule.routing.outbound.IteratorMessageSequence;

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
        Object payload = event.getMessage().getPayload();
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
        else
        {
            throw new IllegalArgumentException(CoreMessages.objectNotOfCorrectType(payload.getClass(),
                new Class[]{Iterable.class, Iterator.class, MessageSequence.class, Collection.class})
                .getMessage());
        }
    }
}
