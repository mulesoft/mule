/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.paging;

import org.mule.api.Closeable;
import org.mule.api.MuleException;
import org.mule.api.paging.Consumer;
import org.mule.api.paging.Producer;

import java.util.Iterator;

public class ProducerConsumerIterator<T> implements Iterator<T>, Closeable
{

    private Producer<T> producer;
    private Consumer<T> consumer;

    public ProducerConsumerIterator(Producer<T> producer, Consumer<T> consumer)
    {
        this.producer = producer;
        this.consumer = consumer;
    }

    @Override
    public void close() throws MuleException
    {
        this.consumer.close();
        this.producer.close();
    }

    @Override
    public boolean hasNext()
    {
        return !this.consumer.isConsumed(this.producer);
    }

    @Override
    public T next()
    {
        return this.consumer.consume(this.producer);
    }

    public void remove()
    {
        throw new UnsupportedOperationException();
    }

}
