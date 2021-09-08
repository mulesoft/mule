/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing.outbound;

import static java.util.Objects.requireNonNull;

import org.mule.runtime.core.internal.routing.AbstractMessageSequence;
import org.mule.runtime.core.internal.routing.MessageSequence;

import java.util.Collection;
import java.util.Iterator;

/**
 * A {@link MessageSequence} that retrieves elements from a {@link Collection}. Its estimated size is initially the size of the
 * collection, and decreases when elements are consumed using {@link #next()}
 * 
 * @author flbulgarelli
 * @param <T>
 */
public final class CollectionMessageSequence<T> extends AbstractMessageSequence<T> {

  private final Iterator<T> iter;
  private int remaining;

  public CollectionMessageSequence(Collection<T> collection) {
    requireNonNull(collection);
    if (collection instanceof EventBuilderConfigurerList) {
      this.iter = ((EventBuilderConfigurerList) collection).eventBuilderConfigurerIterator();
    } else {
      this.iter = collection.iterator();
    }
    this.remaining = collection.size();
  }

  @Override
  public Integer size() {
    return remaining;
  }

  @Override
  public boolean hasNext() {
    return iter.hasNext();
  }

  @Override
  public T next() {
    T next = iter.next();
    remaining--;
    return next;
  }

}
