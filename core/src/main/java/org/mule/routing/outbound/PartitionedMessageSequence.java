/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing.outbound;

import org.mule.api.MuleRuntimeException;
import org.mule.config.i18n.MessageFactory;
import org.mule.routing.MessageSequence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;

/**
 * A {@link MessageSequence} wrapper that partitions the wrapped sequence in
 * collections of the specified size.
 */
public class PartitionedMessageSequence<Q> implements MessageSequence<Collection<Q>>
{
    private MessageSequence<Q> delegate;
    private int groupSize;

    public PartitionedMessageSequence(MessageSequence<Q> seq, int groupSize)
    {
        if (groupSize <= 1)
        {
            throw new MuleRuntimeException(MessageFactory.createStaticMessage("group size must be greater than 1"));
        }
        this.delegate = seq;
        this.groupSize = groupSize;
    }

    @Override
    public int size()
    {
        return (delegate.size() / groupSize) + ((delegate.size() % groupSize) > 0 ? 1 : 0);
    }

    @Override
    public boolean hasNext()
    {
        return delegate.hasNext();
    }

    @Override
    public Collection<Q> next()
    {
        if (!delegate.hasNext())
        {
            throw new NoSuchElementException();
        }
        Collection<Q> batch = new ArrayList<Q>();
        int i = groupSize;
        while (i > 0 && delegate.hasNext())
        {
            batch.add(delegate.next());
            i--;
        }
        return batch;
    }

    @Override
    public boolean isEmpty()
    {
        return !hasNext();
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();

    }

}


