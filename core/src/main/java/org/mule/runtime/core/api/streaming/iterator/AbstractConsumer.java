/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.streaming.iterator;

import org.mule.runtime.core.internal.streaming.object.iterator.ClosedConsumerException;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base implementation of {@link Consumer}. This template class takes care of the contract's subtleties like returning fast when
 * closed and throwing the correct types of exceptions
 *
 * @param <T> the type that the consumer will return
 * @param <P> the type that the producer generates
 * @since 3.5.0
 */
public abstract class AbstractConsumer<T, P> implements Consumer<T> {

  private static final transient Logger logger = LoggerFactory.getLogger(AbstractConsumer.class);

  protected final Producer<P> producer;
  private boolean closed = false;

  public AbstractConsumer(Producer<P> producer) {
    this.producer = producer;
  }

  /**
   * Implement this method to actually consume the producer without worrying about exception types or checking fo this consumer to
   * be closed. If the producer is consumed then simply return <code>null</code>
   *
   * @return a new item or <code>null</code>
   * @throws NoSuchElementException
   */
  protected abstract T doConsume() throws NoSuchElementException;

  /**
   * Implement this method to actualy check for the {@link Producer} being fully consumed without worrying about it being closed
   * or throwing any exceptions
   *
   * @return whether the {@link Producer} has been consumed or not
   */
  protected abstract boolean checkConsumed();

  /**
   * {@inheritDoc}
   */
  @Override
  public final T consume() throws NoSuchElementException {
    if (this.closed) {
      throw new ClosedConsumerException("this consumer is already closed");
    }

    if (this.isConsumed()) {
      throw new NoSuchElementException();
    }

    T value = this.doConsume();

    if (value == null) {
      this.closeQuietly();
    }

    return value;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final boolean isConsumed() {
    if (this.closed) {
      return true;
    }

    boolean isConsumed = this.checkConsumed();
    if (isConsumed) {
      this.closeQuietly();
    }
    return isConsumed;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() throws IOException {
    this.closed = true;
    this.producer.close();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getSize() {
    return this.producer.getSize();
  }

  private void closeQuietly() {
    try {
      this.close();
    } catch (Exception e) {
      if (logger.isWarnEnabled()) {
        logger.warn("Exception was trapped trying to close consumer", e);
      }
    }
  }

}
