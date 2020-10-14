/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import org.mule.runtime.core.api.management.stats.PayloadStatistics;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.ObjLongConsumer;

import org.apache.commons.collections.list.AbstractListDecorator;

final class PayloadStatisticsList<T> extends AbstractListDecorator {

  private final PayloadStatistics statistics;
  private final ObjLongConsumer<PayloadStatistics> populator;

  PayloadStatisticsList(List<T> decorated, PayloadStatistics statistics, ObjLongConsumer<PayloadStatistics> populator) {
    super(decorated);
    this.statistics = statistics;
    this.populator = populator;
  }

  @Override
  public Iterator iterator() {
    return new PayloadStatisticsIterator<>(super.iterator(), statistics, populator);
  }

  @Override
  public Spliterator spliterator() {
    return Spliterators.spliterator(iterator(), size(), 0);
  }

  @Override
  public Object[] toArray() {
    populator.accept(statistics, size());
    return super.toArray();
  }

  @Override
  public Object[] toArray(Object[] object) {
    populator.accept(statistics, size());
    return super.toArray(object);
  }

}
