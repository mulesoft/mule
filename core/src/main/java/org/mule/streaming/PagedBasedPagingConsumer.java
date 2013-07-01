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

import org.mule.api.MuleException;
import org.mule.api.streaming.Consumer;
import org.mule.api.streaming.Producer;

import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PagedBasedPagingConsumer<T> implements Consumer<T>
{

    private static transient final Logger logger = LoggerFactory.getLogger(PagedBasedPagingConsumer.class);

    private Producer<T> producer;
    private List<T> currentPage = null;

    public PagedBasedPagingConsumer(Producer<T> producer)
    {
        this.producer = producer;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T consume() throws NoSuchElementException
    {
        if (this.isConsumed())
        {
            throw new NoSuchElementException();
        }

        return (T) this.currentPage;
    }

    @Override
    public boolean isConsumed()
    {
        if (CollectionUtils.isEmpty(this.currentPage))
        {
            this.loadNextPage(this.producer);
        }

        if (CollectionUtils.isEmpty(this.currentPage))
        {
            try
            {
                this.close();
            }
            catch (MuleException e)
            {
                if (logger.isWarnEnabled())
                {
                    logger.warn("Expection was trapped trying to close consumer", e);
                }
            }
            return true;
        }

        return false;
    }

    @Override
    public void close() throws MuleException
    {
        this.currentPage = null;
        this.producer.close();
    }

    private void loadNextPage(Producer<T> producer)
    {
        this.currentPage = producer.produce();
    }
}
