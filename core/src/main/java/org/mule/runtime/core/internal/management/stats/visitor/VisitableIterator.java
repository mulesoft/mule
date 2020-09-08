/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats.visitor;

import java.util.Iterator;

import org.mule.runtime.api.streaming.HasSize;

/**
 * An iterator that can be visit to decorate.
 *
 * @since 4.4, 4.3.1
 */
public class VisitableIterator<T> implements Iterator<T>, Visitable<Iterator<T>>, HasSize {

  private final Iterator<T> delegate;

  public VisitableIterator(Iterator<T> delegate) {
    this.delegate = delegate;
  }

  @Override
  public boolean hasNext() {
    return delegate.hasNext();
  }

  @Override
  public T next() {
    return delegate.next();
  }

  @Override
  public Iterator<T> accept(Visitor visitor) {
    return visitor.visitIterator(this);
  }

  @Override
  public Iterator<T> getDelegate() {
    return delegate;
  }

  @Override
  public int getSize() {
    return delegate instanceof HasSize ? ((HasSize) delegate).getSize() : 0;
  }

}
