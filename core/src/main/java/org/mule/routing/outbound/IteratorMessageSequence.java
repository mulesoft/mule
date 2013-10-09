/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing.outbound;

import java.util.Iterator;

import org.apache.commons.lang.Validate;
import org.mule.routing.AbstractMessageSequence;
import org.mule.routing.MessageSequence;

/**
 * A {@link MessageSequence} that delegates its {@link #hasNext()} and
 * {@link #next()} methods to an {@link Iterator}, and has no estimated size
 * 
 * @author flbulgarelli
 * @param <T>
 */
public final class IteratorMessageSequence<T> extends AbstractMessageSequence<T>
{
    private final Iterator<T> iter;

    public IteratorMessageSequence(Iterator<T> iter)
    {
        Validate.notNull(iter);
        this.iter = iter;
    }

    public int size()
    {
        return UNKNOWN_SIZE;
    }

    public boolean hasNext()
    {
        return iter.hasNext();
    }

    public T next()
    {
        return iter.next();
    }

}
