/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.streaming.object.iterator;

import org.mule.runtime.core.api.streaming.iterator.Producer;
import org.mule.runtime.core.api.util.queue.Queue;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link Producer} to stream the contents of a {@link Queue} A polling timeout
 * value might be specified, otherwise the default value of 5000 milliseconds will be assumed
 */
public class QueueProducer<T> implements Producer<T> {

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
  public QueueProducer(Queue queue) {
    this(queue, DEFAULT_TIMEOUT_VALUE);
  }

  public QueueProducer(Queue queue, long timeout) {
    if (queue == null) {
      throw new IllegalArgumentException("Cannot make a producer out of a null queue");
    }
    this.queue = queue;
    this.size = queue.size();
    this.timeout = timeout;
  }

  /**
   * {@inheritDoc} This implementation will poll from the queue once and will return the obtained item. If the producer is closed
   * or if the queue times out while polling, then <code>null</code> is returned. If the poll method throws
   * {@link InterruptedException} then <code>null</code> is returned as well
   */
  @Override
  @SuppressWarnings("unchecked")
  public T produce() {
    if (this.queue == null) {
      return null;
    }

    T item = null;
    try {
      item = (T) this.queue.poll(this.timeout);
    } catch (InterruptedException e) {
      logger.warn("Thread interrupted while polling in producer. Will return an empty list", e);
    }

    return item;
  }

  @Override
  public void close() throws IOException {
    this.queue = null;
  }

  @Override
  public int getSize() {
    return this.size;
  }

}
