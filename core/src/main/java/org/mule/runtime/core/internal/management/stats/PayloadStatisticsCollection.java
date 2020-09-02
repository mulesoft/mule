/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.LongConsumer;

import org.apache.commons.collections.collection.AbstractCollectionDecorator;

class PayloadStatisticsCollection<T> extends AbstractCollectionDecorator {

  private final LongConsumer populator;

  PayloadStatisticsCollection(Collection<T> decorated, LongConsumer populator) {
    super(decorated);
    this.populator = populator;
  }

  @Override
  public Iterator iterator() {
    return new PayloadStatisticsIterator<>(super.iterator(), populator);
  }

  @Override
  public Spliterator spliterator() {
    return Spliterators.spliterator(iterator(), size(), 0);
  }

  @Override
  public Object[] toArray() {
    populator.accept(size());
    return super.toArray();
  }

  @Override
  public Object[] toArray(Object[] object) {
    populator.accept(size());
    return super.toArray(object);
  }
}
