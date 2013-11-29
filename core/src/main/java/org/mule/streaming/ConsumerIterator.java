/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.streaming;

import org.mule.api.Closeable;
import org.mule.api.MuleException;

import java.util.Iterator;

/**
 * Implementation of {@link java.util.Iterator} that takes its elements from a
 * {@link org.mule.streaming.Consumer}. This iterator also implements
 * {@link org.mule.api.Closeable}. Closing this iterator will cause the underlying
 * consumer to be closed. If for any reason the underlying consumer gets closed
 * (either because this iterator closed it or some other reason), then this iterator
 * will consider that it has not next items. remove() operation is not allowed on
 * this instance
 */
public class ConsumerIterator<T> implements Iterator<T>, Closeable, ProvidesTotalHint
{

    private Consumer<T> consumer;

    /**
     * Creates a new ConsumerIterator for the given {@link Producer}. Use this
     * factory method over the {@link #ConsumerIterator(Consumer)} constructor
     * whenever you want to let the system choose what the best {@link Consumer} is
     * for your case
     * 
     * @param producer a {@link Producer}
     * @return a new {@link ConsumerIterator}
     */
    public static <T> ConsumerIterator<T> forProducer(Producer<T> producer)
    {
        return new ConsumerIterator<T>(new ElementBasedPagingConsumer<T>(producer));
    }

    public ConsumerIterator(Consumer<T> consumer)
    {
        this.consumer = consumer;
    }

    /**
     * Closes the underlying consumer
     */
    @Override
    public void close() throws MuleException
    {
        this.consumer.close();
    }

    /**
     * Returns true as long as the underlying consumer is not fully consumed nor
     * closed
     */
    @Override
    public boolean hasNext()
    {
        return !this.consumer.isConsumed();
    }

    /**
     * Gets an item from the consumer and returns it
     */
    @Override
    public T next()
    {
        return this.consumer.consume();
    }

    /**
     * Not allowed on this implementations
     * 
     * @throws UnsupportedOperationException
     */
    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    public int size()
    {
        return this.consumer.size();
    }

}
