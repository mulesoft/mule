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

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link org.mule.streaming.Consumer} that obains data from an
 * instance of {@link org.mule.streaming.Producer} and returns the elements in
 * the same pages as the producer returns them. This implementation is not
 * thread-safe
 */
public class PagedBasedPagingConsumer<T> implements Consumer<T>
{

    private static transient final Logger logger = LoggerFactory.getLogger(PagedBasedPagingConsumer.class);

    private Producer<T> producer;
    private List<T> currentPage = null;
    private boolean closed = false;

    public PagedBasedPagingConsumer(Producer<T> producer)
    {
        this.producer = producer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public T consume() throws NoSuchElementException
    {
        if (this.closed)
        {
            throw new ClosedConsumerException("this consumer is already closed");
        }

        if (this.isConsumed())
        {
            throw new NoSuchElementException();
        }

        T page = (T) this.currentPage;
        this.currentPage = null;

        return page;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isConsumed()
    {
        if (this.closed)
        {
            return true;
        }

        if (CollectionUtils.isEmpty(this.currentPage))
        {
            this.loadNextPage(this.producer);

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
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int totalAvailable()
    {
        return this.producer.totalAvailable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws MuleException
    {
        this.closed = true;
        this.currentPage = null;
        this.producer.close();
    }

    private void loadNextPage(Producer<T> producer)
    {
        this.currentPage = producer.produce();
    }
}
