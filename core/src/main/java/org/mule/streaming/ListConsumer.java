/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.streaming;

import org.mule.api.MuleException;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Implementation of {@link org.mule.streaming.Consumer} that obains a {@link List}
 * from a {@link Producer} and returns the elements one by one. This implementation
 * is not thread-safe.
 * 
 * @since 3.5.0
 */
public class ListConsumer<T> extends AbstractConsumer<T, List<T>>
{

    private List<T> currentPage = null;
    private int index;
    private int pageSize;

    public ListConsumer(Producer<List<T>> producer)
    {
        super(producer);
        this.reset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected T doConsume() throws NoSuchElementException
    {
        if (this.isConsumed())
        {
            throw new NoSuchElementException();
        }

        T element = this.currentPage.get(this.index);
        this.index++;

        return element;
    }

    @Override
    protected boolean checkConsumed()
    {
        if (this.index >= this.pageSize)
        {
            this.loadNextPage(this.producer);
            return this.pageSize == 0;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size()
    {
        return this.producer.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws MuleException
    {
        super.close();
        this.currentPage = null;
    }

    private void reset()
    {
        this.index = 0;
        this.pageSize = this.currentPage == null ? 0 : this.currentPage.size();
    }

    private void loadNextPage(Producer<List<T>> producer)
    {
        this.currentPage = producer.produce();
        this.reset();
    }
}
