/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.streaming.object.iterator;

import org.mule.runtime.core.api.streaming.iterator.Producer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

/**
 * Implementation of {@link Producer} to expose streams from several producers as a single data feed. Producers are consumed in
 * order until they're all consumed. All producers need to share the same generic type T
 * 
 * @since 3.5.0
 */
public class CompositeProducer<T> implements Producer<T> {

  private Integer totalAvailable = null;
  private List<Producer<T>> producers;
  private Iterator<Producer<T>> producersIterator;
  private Producer<T> currentProducer;

  /**
   * Takes a list of producers to be composited. They will be consumed in this order
   * 
   * @param producers a list of {@link Producer}
   */
  public CompositeProducer(List<Producer<T>> producers) {
    if (CollectionUtils.isEmpty(producers)) {
      throw new IllegalArgumentException("Cannot make a composition of null or empty producers list");
    }

    this.producers = producers;
    this.producersIterator = producers.iterator();
    this.currentProducer = this.producersIterator.next();
  }

  public CompositeProducer(Producer<T>... producers) {
    this(Arrays.asList(producers));
  }

  /**
   * {@inheritDoc} This method calls the produce method on the current producer. When that producer is exhausted, then it switches
   * to the next one. When the last producer is also exhausted, then it returns <code>null<c/code>. This method does not close any
   * producer when it is exhausted. Use the close method for that
   */
  @Override
  public T produce() {
    if (this.currentProducer == null) {
      return null;
    }

    T next = this.currentProducer.produce();
    if (next == null) {

      if (this.producersIterator.hasNext()) {
        this.currentProducer = this.producersIterator.next();
      } else {
        this.currentProducer = null;
      }

      return this.produce();
    }

    return next;
  }

  /**
   * Accumulates the total available count of all the producers. If one of them does not have a value available (returns -1) then
   * it is not factored in
   */
  @Override
  public int getSize() {
    if (this.totalAvailable == null) {
      int total = 0;
      for (Producer<T> producer : this.producers) {
        int available = producer.getSize();
        if (available > 0) {
          total += available;
        }
      }
      this.totalAvailable = total;
    }

    return this.totalAvailable;
  }

  /**
   * Closes all the producers
   */
  @Override
  public void close() throws IOException {
    for (Producer<T> producer : this.producers) {
      producer.close();
    }
  }
}
