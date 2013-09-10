/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.collection;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.config.i18n.CoreMessages;
import org.mule.routing.MessageSequence;
import org.mule.routing.outbound.ArrayMessageSequence;
import org.mule.routing.outbound.CollectionMessageSequence;
import org.mule.routing.outbound.IteratorMessageSequence;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

public class EventToMessageSequenceSplittingStrategy
    implements SplittingStrategy<MuleEvent, MessageSequence<?>>
{

    @SuppressWarnings({"unchecked", "rawtypes"})
    public MessageSequence<?> split(MuleEvent event)
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
    };

}
