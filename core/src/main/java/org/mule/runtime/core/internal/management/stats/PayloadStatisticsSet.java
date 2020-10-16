/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.LongConsumer;

import org.apache.commons.collections.set.AbstractSetDecorator;

final class PayloadStatisticsSet<T> extends AbstractSetDecorator implements Externalizable {

  private final LongConsumer populator;

  public PayloadStatisticsSet() {
    // This is needed for serialization engines
    // like kryo.
    super(new HashSet());
    this.populator = l -> {
      return;
    };
  }

  PayloadStatisticsSet(Set<T> decorated, LongConsumer populator) {
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

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    populator.accept(size());
    out.writeObject(getSet());
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    this.collection = (Set) in.readObject();
  }
}
