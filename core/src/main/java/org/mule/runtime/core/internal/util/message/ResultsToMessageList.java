/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.message;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Wraps a {@link List} of {@link Result} instances and exposes
 * its contents as {@link Message} instances.
 * <p>
 * This allows to avoid preemptive transformations of an entire List
 * of {@link Result} to {@link Message}
 *
 * @since 4.0
 */
public final class ResultsToMessageList extends ResultsToMessageCollection implements List<Message> {

  private final List<Object> delegate;

  public ResultsToMessageList(List<Object> delegate,
                              CursorProviderFactory cursorProviderFactory,
                              CoreEvent event) {
    super(delegate, cursorProviderFactory, event);
    this.delegate = delegate;
  }

  @Override
  public void add(int index, Message element) {
    lock.withWriteLock(() -> delegate.add(index, element));
  }

  @Override
  public boolean addAll(int index, Collection<? extends Message> c) {
    return lock.withWriteLock(() -> delegate.addAll(index, c));
  }

  @Override
  public int indexOf(Object o) {
    return lock.withReadLock(r -> {
      int i = delegate.indexOf(o);
      if (i == -1 && o instanceof Message) {
        i = delegate.indexOf(Result.builder((Message) o).build());
      }

      return i;
    });
  }

  @Override
  public int lastIndexOf(Object o) {
    return lock.withReadLock(r -> {
      int i = delegate.lastIndexOf(o);
      if (i == -1 && o instanceof Message) {
        i = delegate.lastIndexOf(Result.builder((Message) o).build());
      }

      return i;
    });
  }

  @Override
  public void sort(Comparator<? super Message> c) {
    lock.withWriteLock(() -> delegate.sort((o1, o2) -> c.compare(toMessage(o1, cursorProviderFactory, event),
                                                                 toMessage(o2, cursorProviderFactory, event))));
  }

  @Override
  public Message get(int index) {
    return (Message) lock.withReadLock(r -> {
      Object value = delegate.get(index);
      if (value instanceof Message) {
        return value;
      }
      r.release();
      return lock.withWriteLock(() -> {
        Object update = delegate.get(index);
        if (update instanceof Message) {
          return update;
        }
        update = toMessage(update, cursorProviderFactory, event);
        delegate.set(index, update);
        return update;
      });
    });
  }

  @Override
  public Message set(int index, Message message) {
    return lock.withWriteLock(() -> {
      Object previous = delegate.set(index, message);
      return previous != null ? toMessage(previous, cursorProviderFactory, event) : null;
    });
  }

  @Override
  public Message remove(int index) {
    return lock.withWriteLock(() -> {
      Object previous = delegate.remove(index);
      return previous != null ? toMessage(previous, cursorProviderFactory, event) : null;
    });
  }

  @Override
  public Iterator<Message> iterator() {
    return listIterator();
  }

  @Override
  public ListIterator<Message> listIterator() {
    return new ResultToMessageListIterator(this);
  }

  @Override
  public ListIterator<Message> listIterator(int index) {
    return new ResultToMessageListIterator(this);
  }

  @Override
  public List<Message> subList(int fromIndex, int toIndex) {
    return lock.withReadLock(r -> {
      List results = delegate.subList(fromIndex, toIndex);
      return new ResultsToMessageList(results, cursorProviderFactory, event);
    });
  }

}
