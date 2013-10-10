/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
