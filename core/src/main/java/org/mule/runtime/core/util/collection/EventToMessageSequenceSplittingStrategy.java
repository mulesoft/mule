/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.collection;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.util.Copiable;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.routing.MessageSequence;
import org.mule.runtime.core.routing.outbound.ArrayMessageSequence;
import org.mule.runtime.core.routing.outbound.CollectionMessageSequence;
import org.mule.runtime.core.routing.outbound.IteratorMessageSequence;
import org.mule.runtime.core.routing.outbound.NodeListMessageSequence;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.w3c.dom.NodeList;

public class EventToMessageSequenceSplittingStrategy
        implements SplittingStrategy<MuleEvent, MessageSequence<?>>
{

    @SuppressWarnings({"unchecked", "rawtypes"})
    public MessageSequence<?> split(MuleEvent event)
    {
        MuleMessage msg = event.getMessage();
        Object payload = msg.getPayload();
        if (payload instanceof MessageSequence<?>)
        {
            return ((MessageSequence<?>) payload);
        }
        if (payload instanceof Iterator<?>)
        {
            return new IteratorMessageSequence<>(((Iterator<Object>) payload));
        }
        if (payload instanceof Collection)
        {
            return new CollectionMessageSequence(copyCollection((Collection) payload));
        }
        if (payload instanceof Iterable<?>)
        {
            return new IteratorMessageSequence<>(((Iterable<Object>) payload).iterator());
        }
        if (payload instanceof Object[])
        {
            return new ArrayMessageSequence((Object[]) payload);
        }
        else if (payload instanceof NodeList)
        {
            return new NodeListMessageSequence((NodeList) payload);
        }
        else
        {
            throw new IllegalArgumentException(CoreMessages.objectNotOfCorrectType(payload.getClass(),
                                                                                   new Class[] {Iterable.class, Iterator.class, MessageSequence.class, Collection.class})
                                                       .getMessage());
        }
    }

    private Collection copyCollection(Collection payload)
    {
        return payload instanceof Copiable
               ? ((Copiable<Collection>) payload).copy()
               : new LinkedList(payload);
    }


}
