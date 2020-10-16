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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.LongConsumer;

import org.apache.commons.collections.list.AbstractListDecorator;

final class PayloadStatisticsList<T> extends AbstractListDecorator implements Externalizable {

  private static final long serialVersionUID = -5991764233175594789L;

  private final LongConsumer populator;

  public PayloadStatisticsList() {
    // This is needed for serialization engines
    // like kryo.
    super(new ArrayList());
    this.populator = l -> {
      return;
    };
  }

  PayloadStatisticsList(List<T> decorated, LongConsumer populator) {
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
    out.writeObject(getList());
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    this.collection = (List) in.readObject();
  }

}
