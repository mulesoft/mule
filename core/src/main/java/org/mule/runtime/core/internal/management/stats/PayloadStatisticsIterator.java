/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import org.mule.runtime.api.streaming.HasSize;
import org.mule.runtime.core.api.management.stats.PayloadStatistics;

import java.util.Iterator;
import java.util.function.ObjLongConsumer;

import org.apache.commons.collections.iterators.AbstractIteratorDecorator;

final class PayloadStatisticsIterator<T> extends AbstractIteratorDecorator implements HasSize {

  private final PayloadStatistics statistics;
  private final ObjLongConsumer<PayloadStatistics> populator;

  PayloadStatisticsIterator(Iterator<T> iterator, PayloadStatistics statistics, ObjLongConsumer<PayloadStatistics> populator) {
    super(iterator);
    this.statistics = statistics;
    this.populator = populator;
  }

  @Override
  public T next() {
    final Object next = super.next();
    populator.accept(statistics, 1);
    return (T) next;
  }

  @Override
  public int getSize() {
    return getIterator() instanceof HasSize ? ((HasSize) getIterator()).getSize() : -1;
  }
}
