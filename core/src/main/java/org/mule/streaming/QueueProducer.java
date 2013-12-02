/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.streaming;

import org.mule.api.MuleException;
import org.mule.util.queue.Queue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link Producer} to stream the contents of a
 * {@link org.mule.util.queue.Queue} A polling timeout value might be specified,
 * otherwise the default value of 5000 milliseconds will be assumed
 */
public class QueueProducer<T> implements Producer<T>
{

    private static final Logger logger = LoggerFactory.getLogger(QueueProducer.class);
    private static final long DEFAULT_TIMEOUT_VALUE = 5000;

    private Queue queue;
    private int size;
    private long timeout;

    /**
     * Creates an instance with 5000 milliseconds as the default polling value
     * 
     * @param queue the queue to stream from
     */
    public QueueProducer(Queue queue)
    {
        this(queue, DEFAULT_TIMEOUT_VALUE);
    }

    public QueueProducer(Queue queue, long timeout)
    {
        if (queue == null)
        {
            throw new IllegalArgumentException("Cannot make a producer out of a null queue");
        }
        this.queue = queue;
        this.size = queue.size();
        this.timeout = timeout;
    }

    /**
     * {@inheritDoc} This implementation will poll from the queue once and will
     * return the obtained item in a single element list. If the producer is closed
     * or if the queue times out while polling, then an empty list is returned. If
     * the poll method throws {@link InterruptedException} then an empty list is
     * returned as well
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<T> produce()
    {
        if (this.queue == null)
        {
            return Collections.emptyList();
        }

        T item = null;
        try
        {
            item = (T) this.queue.poll(this.timeout);
        }
        catch (InterruptedException e)
        {
            logger.warn("Thread interrupted while polling in producer. Will return an empty list", e);
        }

        return item != null ? Arrays.<T>asList(item) : Collections.<T> emptyList();
    }

    @Override
    public void close() throws MuleException
    {
        this.queue = null;
    }

    @Override
    public int size()
    {
        return this.size;
    }

}
