/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.management.stats;

import java.util.Collection;
import java.util.Iterator;

/**
 * A collection that can be visit to be decorated.
 *
 * @since 4.4, 4.3.1
 */
public class VisitableCollection implements Visitable<Collection>, Collection {

  private final Collection delegate;

  public VisitableCollection(Collection delegate) {
    this.delegate = delegate;
  }

  @Override
  public Collection accept(Visitor visitor) {
    return visitor.visitCollection(this);
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return delegate.contains(o);
  }

  @Override
  public Iterator iterator() {
    return delegate.iterator();
  }

  @Override
  public Object[] toArray() {
    return delegate.toArray();
  }

  @Override
  public Object[] toArray(Object[] a) {
    return delegate.toArray(a);
  }

  @Override
  public boolean add(Object e) {
    return delegate.add(e);
  }

  @Override
  public boolean remove(Object o) {
    return delegate.remove(o);
  }

  @Override
  public boolean containsAll(Collection c) {
    return delegate.containsAll(c);
  }

  @Override
  public boolean addAll(Collection c) {
    return delegate.addAll(c);
  }

  @Override
  public boolean removeAll(Collection c) {
    return delegate.removeAll(c);
  }

  @Override
  public boolean retainAll(Collection c) {
    return delegate.retainAll(c);
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  @Override
  public Collection getDelegate() {
    return delegate;
  }

}
