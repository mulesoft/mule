/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.streaming;

import org.mule.api.Closeable;
import org.mule.api.MuleException;
import org.mule.api.streaming.Consumer;

import java.util.Iterator;

public class ConsumerIterator<T> implements Iterator<T>, Closeable
{

    private Consumer<T> consumer;

    public ConsumerIterator(Consumer<T> consumer)
    {
        this.consumer = consumer;
    }

    @Override
    public void close() throws MuleException
    {
        this.consumer.close();
    }

    @Override
    public boolean hasNext()
    {
        return !this.consumer.isConsumed();
    }

    @Override
    public T next()
    {
        return this.consumer.consume();
    }

    public void remove()
    {
        throw new UnsupportedOperationException();
    }

}
