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

import org.apache.commons.collections.CollectionUtils;

public class PagedBasedPagingConsumer<T> implements Consumer<T>
{

    private List<T> currentPage = null;

    @Override
    @SuppressWarnings("unchecked")
    public T consume(Producer<T> producer) throws NoSuchElementException
    {
        if (this.isConsumed(producer))
        {
            throw new NoSuchElementException();
        }

        return (T) this.currentPage;
    }

    @Override
    public boolean isConsumed(Producer<T> producer)
    {
        if (CollectionUtils.isEmpty(this.currentPage))
        {
            this.loadNextPage(producer);
        }

        return !CollectionUtils.isEmpty(this.currentPage);
    }

    @Override
    public void close() throws MuleException
    {
        this.currentPage = null;
    }

    private void loadNextPage(Producer<T> producer)
    {
        this.currentPage = producer.produce();
    }
}
