/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing.outbound;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.lang.Validate;
import org.mule.routing.AbstractMessageSequence;
import org.mule.routing.MessageSequence;

/**
 * A {@link MessageSequence} that retrieves elements from a {@link Collection}. Its
 * estimated size is initially the size of the collection, and decreases when
 * elements are consumed using {@link #next()}
 * 
 * @author flbulgarelli
 * @param <T>
 */
public final class CollectionMessageSequence<T> extends AbstractMessageSequence<T>
{
    private final Iterator<T> iter;
    private int remaining;

    public CollectionMessageSequence(Collection<T> collection)
    {
        Validate.notNull(collection);
        this.iter = collection.iterator();
        this.remaining = collection.size();
    }

    public int size()
    {
        return remaining;
    }

    public boolean hasNext()
    {
        return iter.hasNext();
    }

    public T next()
    {
        T next = iter.next();
        remaining--;
        return next;
    }

}
