/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing.outbound;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.core.internal.routing.MessageSequence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;

/**
 * A {@link MessageSequence} wrapper that partitions the wrapped sequence in collections of the specified size.
 */
public class PartitionedMessageSequence<Q> implements MessageSequence<Collection<Q>> {

  private MessageSequence<Q> delegate;
  private int groupSize;

  public PartitionedMessageSequence(MessageSequence<Q> seq, int groupSize) {
    if (groupSize <= 1) {
      throw new MuleRuntimeException(I18nMessageFactory.createStaticMessage("group size must be greater than 1"));
    }
    this.delegate = seq;
    this.groupSize = groupSize;
  }

  @Override
  public Integer size() {
    return delegate.size() == null ? null : (delegate.size() / groupSize) + ((delegate.size() % groupSize) > 0 ? 1 : 0);
  }

  @Override
  public boolean hasNext() {
    return delegate.hasNext();
  }

  @Override
  public Collection<Q> next() {
    if (!delegate.hasNext()) {
      throw new NoSuchElementException();
    }
    Collection<Q> batch = new ArrayList<>();
    int i = groupSize;
    while (i > 0 && delegate.hasNext()) {
      batch.add(delegate.next());
      i--;
    }
    return batch;
  }

  @Override
  public boolean isEmpty() {
    return !hasNext();
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();

  }

}


