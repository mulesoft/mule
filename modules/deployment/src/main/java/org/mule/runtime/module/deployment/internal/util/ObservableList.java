/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.util;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Adapted version of a groovy.util.ObservableList. List decorator that will trigger PropertyChangeEvents when a value
 * changes.<br>
 * <p/>
 * The current implementation will trigger specialized events in the following scenarios, you need not register a different
 * listener as those events extend from PropertyChangeEvent
 * <ul>
 * <li>ObservableList.ElementAddedEvent - a new element is added to the list</li>
 * <li>ObservableList.ElementRemovedEvent - a element is removed from the list</li>
 * <li>ObservableList.ElementUpdatedEvent - a element changes value (same as regular PropertyChangeEvent)</li>
 * <li>ObservableList.ElementClearedEvent - all elements have been removed from the list</li>
 * <li>ObservableList.MultiElementAddedEvent - triggered by calling list.addAll()</li>
 * <li>ObservableList.MultiElementRemovedEvent - triggered by calling list.removeAll()/list.retainAll()</li>
 * </ul>
 * </p>
 */
public class ObservableList<E> implements List<E> {

  private List<E> delegate;
  private PropertyChangeSupport pcs;

  public ObservableList() {
    this(new ArrayList<E>());
  }

  public ObservableList(List<E> delegate) {
    this.delegate = delegate;
    pcs = new PropertyChangeSupport(this);
  }

  @Override
  public synchronized void add(int index, E element) {
    delegate.add(index, element);
    pcs.firePropertyChange(new ElementAddedEvent(this, element, index));
  }

  @Override
  public synchronized boolean addAll(Collection<? extends E> c) {
    int index = size() - 1;
    index = index < 0 ? 0 : index;

    boolean success = delegate.addAll(c);
    if (success && c != null) {
      List<E> values = new ArrayList<E>();
      for (E element : values) {
        values.add(element);
      }

      if (values.size() > 0) {
        pcs.firePropertyChange(new MultiElementAddedEvent(this, index, values));
      }
    }

    return success;
  }

  @Override
  public synchronized boolean addAll(int index, Collection<? extends E> c) {
    boolean success = delegate.addAll(index, c);

    if (success && c != null) {
      List<E> values = new ArrayList<E>();
      for (E element : c) {
        values.add(element);
      }
      if (values.size() > 0) {
        pcs.firePropertyChange(new MultiElementAddedEvent(this, index, values));
      }
    }

    return success;
  }

  @Override
  public synchronized void clear() {
    List<E> values = new ArrayList<E>();
    values.addAll(delegate);
    delegate.clear();
    if (!values.isEmpty()) {
      pcs.firePropertyChange(new ElementsClearedEvent(this, values));
    }
  }

  @Override
  public synchronized boolean contains(Object o) {
    return delegate.contains(o);
  }

  @Override
  public synchronized boolean containsAll(Collection<?> c) {
    return delegate.containsAll(c);
  }

  @Override
  public synchronized boolean equals(Object o) {
    return delegate.equals(o);
  }

  @Override
  public synchronized E get(int index) {
    return delegate.get(index);
  }

  @Override
  public synchronized int hashCode() {
    return delegate.hashCode();
  }

  @Override
  public synchronized int indexOf(Object o) {
    return delegate.indexOf(o);
  }

  @Override
  public synchronized boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public synchronized Iterator<E> iterator() {
    return new ObservableIterator(new ArrayList(delegate).iterator());
  }

  @Override
  public synchronized int lastIndexOf(Object o) {
    return delegate.lastIndexOf(o);
  }

  @Override
  public synchronized ListIterator<E> listIterator() {
    return new ObservableListIterator(new ArrayList(delegate).listIterator(), 0);
  }

  @Override
  public synchronized ListIterator<E> listIterator(int index) {
    return new ObservableListIterator(new ArrayList<E>(delegate).listIterator(index), index);
  }

  @Override
  public synchronized E remove(int index) {
    E element = delegate.remove(index);
    pcs.firePropertyChange(new ElementRemovedEvent(this, element, index));
    return element;
  }

  @Override
  public synchronized boolean remove(Object o) {
    int index = delegate.indexOf(o);
    boolean success = delegate.remove(o);
    if (success) {
      pcs.firePropertyChange(new ElementRemovedEvent(this, o, index));
    }
    return success;
  }

  @Override
  public synchronized boolean removeAll(Collection<?> c) {
    if (c == null) {
      return false;
    }

    List<Object> values = new ArrayList<Object>();
    for (Object element : c) {
      if (delegate.contains(element)) {
        values.add(element);
      }
    }

    boolean success = delegate.removeAll(c);
    if (success && !values.isEmpty()) {
      pcs.firePropertyChange(new MultiElementRemovedEvent(this, values));
    }

    return success;
  }

  @Override
  public synchronized boolean retainAll(Collection<?> c) {
    if (c == null) {
      return false;
    }

    List<E> values = new ArrayList<E>();
    if (c != null) {
      for (E element : delegate) {
        if (!c.contains(element)) {
          values.add(element);
        }
      }
    }

    boolean success = delegate.retainAll(c);
    if (success && !values.isEmpty()) {
      pcs.firePropertyChange(new MultiElementRemovedEvent(this, values));
    }

    return success;
  }

  @Override
  public synchronized E set(int index, E element) {
    E oldValue = delegate.set(index, element);
    pcs.firePropertyChange(new ElementUpdatedEvent(this, oldValue, element, index));
    return oldValue;
  }

  @Override
  public synchronized int size() {
    return delegate.size();
  }

  @Override
  public synchronized List<E> subList(int fromIndex, int toIndex) {
    return delegate.subList(fromIndex, toIndex);
  }

  @Override
  public synchronized Object[] toArray() {
    return delegate.toArray();
  }

  @Override
  public synchronized boolean add(E o) {
    boolean success = delegate.add(o);
    if (success) {
      pcs.firePropertyChange(new ElementAddedEvent(this, o, size() - 1));
    }
    return success;
  }

  @Override
  public synchronized Object[] toArray(Object[] a) {
    return delegate.toArray(a);
  }

  private class ObservableIterator implements Iterator<E> {

    protected static final int LAST_RET_NONE = -1;

    private Iterator<E> iterDelegate;
    protected int cursor = 0;
    protected int lastRet = LAST_RET_NONE;

    public ObservableIterator(Iterator<E> iterDelegate) {
      this.iterDelegate = iterDelegate;
    }

    public Iterator<E> getDelegate() {
      return iterDelegate;
    }

    @Override
    public boolean hasNext() {
      return iterDelegate.hasNext();
    }

    @Override
    public E next() {
      E next = iterDelegate.next();
      lastRet = cursor++;
      return next;
    }

    @Override
    public void remove() {
      if (lastRet < 0) {
        throw new IllegalStateException("No element has been returned yet, or remove() or add() is called after the return.");
      }
      iterDelegate.remove();
      // ObservableList.this.remove(lastRet);
      cursor = lastRet;
      lastRet = LAST_RET_NONE;
    }
  }

  protected class ObservableListIterator extends ObservableIterator implements ListIterator<E> {

    public ObservableListIterator(ListIterator<E> iterDelegate, int index) {
      super(iterDelegate);
      cursor = index;
    }

    public ListIterator<E> getListIterator() {
      return (ListIterator<E>) getDelegate();
    }

    @Override
    public void add(E o) {
      getListIterator().add(o);
      cursor++;
      lastRet = LAST_RET_NONE;
    }

    @Override
    public boolean hasPrevious() {
      return getListIterator().hasPrevious();
    }

    @Override
    public int nextIndex() {
      return getListIterator().nextIndex();
    }

    @Override
    public E previous() {
      E prev = getListIterator().previous();
      lastRet = --cursor;
      return prev;
    }

    @Override
    public int previousIndex() {
      return getListIterator().previousIndex();
    }

    @Override
    public void set(E e) {
      if (lastRet < 0) {
        throw new IllegalStateException("No element has been returned yet, or remove() or add() is called after the return.");
      }
      ObservableList.this.set(lastRet, e);
    }

  }

  // observable interface

  public void addPropertyChangeListener(PropertyChangeListener listener) {
    pcs.addPropertyChangeListener(listener);
  }

  public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
    pcs.addPropertyChangeListener(propertyName, listener);
  }

  public PropertyChangeListener[] getPropertyChangeListeners() {
    return pcs.getPropertyChangeListeners();
  }

  public PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
    return pcs.getPropertyChangeListeners(propertyName);
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
    pcs.removePropertyChangeListener(listener);
  }

  public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
    pcs.removePropertyChangeListener(propertyName, listener);
  }

  public boolean hasListeners(String propertyName) {
    return pcs.hasListeners(propertyName);
  }
}
