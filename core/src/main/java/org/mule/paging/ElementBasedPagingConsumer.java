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

import org.mule.api.MuleException;
import org.mule.api.paging.Consumer;
import org.mule.api.paging.Producer;

import java.util.List;
import java.util.NoSuchElementException;

public class ElementBasedPagingConsumer<T> implements Consumer<T>
{

    private List<T> currentPage = null;
    private int index;
    private int pageSize;

    public ElementBasedPagingConsumer()
    {
        this.reset();
    }

    @Override
    public T consume(Producer<T> producer) throws NoSuchElementException
    {
        if (this.isConsumed(producer))
        {
            throw new NoSuchElementException();
        }

        T element = this.currentPage.get(this.index);
        this.index++;

        return element;
    }

    @Override
    public boolean isConsumed(Producer<T> producer)
    {
        if (this.index >= this.pageSize)
        {
            this.loadNextPage(producer);
            return this.pageSize == 0;
        }

        return false;
    }

    @Override
    public void close() throws MuleException
    {
        this.currentPage = null;
    }

    private void reset()
    {
        this.index = 0;
        this.pageSize = this.currentPage == null ? 0 : this.currentPage.size();
    }

    private void loadNextPage(Producer<T> producer)
    {
        this.currentPage = producer.produce();
        this.reset();
    }
}
