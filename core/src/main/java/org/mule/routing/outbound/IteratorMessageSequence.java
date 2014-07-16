/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
