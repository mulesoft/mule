/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import org.mule.runtime.api.streaming.HasSize;

import java.util.Iterator;
import java.util.function.LongConsumer;

import org.apache.commons.collections.iterators.AbstractIteratorDecorator;

final class PayloadStatisticsIterator<T> extends AbstractIteratorDecorator implements HasSize {

  private final LongConsumer populator;

  PayloadStatisticsIterator(Iterator<T> iterator, LongConsumer populator) {
    super(iterator);
    this.populator = populator;
  }

  @Override
  public T next() {
    final Object next = super.next();
    populator.accept(1);
    return (T) next;
  }

  @Override
  public int getSize() {
    return getIterator() instanceof HasSize ? ((HasSize) getIterator()).getSize() : -1;
  }
}
