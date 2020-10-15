/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import org.mule.runtime.core.api.management.stats.PayloadStatistics;

import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.ObjIntConsumer;

import org.apache.commons.collections.set.AbstractSetDecorator;

final class PayloadStatisticsSet<T> extends AbstractSetDecorator {

  private final PayloadStatistics statistics;
  private final ObjIntConsumer<PayloadStatistics> populator;

  PayloadStatisticsSet(Set<T> decorated, PayloadStatistics statistics, ObjIntConsumer<PayloadStatistics> populator) {
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
