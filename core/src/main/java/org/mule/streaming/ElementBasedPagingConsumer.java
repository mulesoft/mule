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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElementBasedPagingConsumer<T> implements Consumer<T>
{

    private static final transient Logger logger = LoggerFactory.getLogger(ElementBasedPagingConsumer.class);

    private Producer<T> producer;
    private List<T> currentPage = null;
    private int index;
    private int pageSize;

    public ElementBasedPagingConsumer(Producer<T> producer)
    {
        this.producer = producer;
        this.reset();
    }

    @Override
    public T consume() throws NoSuchElementException
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
    public boolean isConsumed()
    {
        if (this.index >= this.pageSize)
        {
            this.loadNextPage(this.producer);
            if (this.pageSize == 0)
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

    @Override
    public void close() throws MuleException
    {
        this.currentPage = null;
        this.producer.close();
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
