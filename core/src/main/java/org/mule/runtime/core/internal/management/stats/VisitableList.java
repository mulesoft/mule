/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * A list that can be visit to be decorated.
 *
 * @since 4.4, 4.3.1
 */
public class VisitableList implements Visitable<List>, List {

  private final List delegate;

  public VisitableList(List delegate) {
    this.delegate = delegate;
  }

  @Override
  public List accept(Visitor visitor) {
    return visitor.visitList(this);
  }


  @Override
  public List getDelegate() {
    return delegate;
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
  public boolean addAll(int index, Collection c) {
    return delegate.addAll(index, c);
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
  public Object get(int index) {
    return delegate.get(index);
  }

  @Override
  public Object set(int index, Object element) {
    return delegate.set(index, element);
  }

  @Override
  public void add(int index, Object element) {
    delegate.add(index, element);
  }

  @Override
  public Object remove(int index) {
    return delegate.remove(index);
  }

  @Override
  public int indexOf(Object o) {
    return delegate.indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    return delegate.lastIndexOf(o);
  }

  @Override
  public ListIterator listIterator() {
    return delegate.listIterator();
  }

  @Override
  public ListIterator listIterator(int index) {
    return delegate.listIterator();
  }

  @Override
  public List subList(int fromIndex, int toIndex) {
    return delegate.subList(fromIndex, toIndex);
  }

}
