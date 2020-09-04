/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import java.util.Iterator;

/**
 * An iterator that can be visit to decorate.
 *
 * @since 4.4, 4.3.1
 */
public class VisitableIterator implements Iterator, Visitable<Iterator> {

  private final Iterator delegate;

  public VisitableIterator(Iterator delegate) {
    this.delegate = delegate;
  }

  @Override
  public boolean hasNext() {
    return delegate.hasNext();
  }

  @Override
  public Object next() {
    return delegate.next();
  }

  @Override
  public Iterator accept(Visitor visitor) {
    return visitor.visitIterator(this);
  }

  @Override
  public Iterator getDelegate() {
    return delegate;
  }

}
