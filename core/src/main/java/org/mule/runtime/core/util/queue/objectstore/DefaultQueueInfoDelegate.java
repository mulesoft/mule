/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.queue.objectstore;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;

/**
 * The default QueueStoreDelegate. This uses a LinkedList to store the members of the queue.
 *
 * @deprecated this class will be removed in Mule 4.0 in favor of the new queue implementation
 */
@Deprecated
public class DefaultQueueInfoDelegate implements TransientQueueInfoDelegate {

  protected final int capacity;
  protected final LinkedList<Serializable> list;

  public DefaultQueueInfoDelegate(int capacity) {
    this.capacity = capacity;
    list = new LinkedList<Serializable>();
  }

  @Override
  public void putNow(Serializable o) {
    synchronized (list) {
      list.addLast(o);
      list.notifyAll();
    }
  }

  @Override
  public boolean offer(Serializable o, int room, long timeout) throws InterruptedException {
    checkInterrupted();
    synchronized (list) {
      if (capacity > 0) {
        if (capacity <= room) {
          throw new IllegalStateException("Can not add more objects than the capacity in one time");
        }
        long l1 = timeout > 0L ? System.currentTimeMillis() : 0L;
        long l2 = timeout;
        while (list.size() >= capacity - room) {
          if (l2 <= 0L) {
            return false;
          }
          list.wait(l2);
          l2 = timeout - (System.currentTimeMillis() - l1);
        }
      }
      if (o != null) {
        list.addLast(o);
      }
      list.notifyAll();
      return true;
    }
  }

  @Override
  public Serializable poll(long timeout) throws InterruptedException {
    checkInterrupted();
    synchronized (list) {
      long l1 = timeout > 0L ? System.currentTimeMillis() : 0L;
      long l2 = timeout;
      while (list.isEmpty()) {
        if (l2 <= 0L) {
          return null;
        }
        list.wait(l2);
        l2 = timeout - (System.currentTimeMillis() - l1);
      }

      Serializable o = list.removeFirst();
      list.notifyAll();
      return o;
    }
  }

  @Override
  public Serializable peek() throws InterruptedException {
    checkInterrupted();
    synchronized (list) {
      if (list.isEmpty()) {
        return null;
      } else {
        return list.getFirst();
      }
    }
  }

  @Override
  public void untake(Serializable item) throws InterruptedException {
    checkInterrupted();
    synchronized (list) {
      list.addFirst(item);
    }
  }

  @Override
  public void clear() throws InterruptedException {
    this.checkInterrupted();
    synchronized (list) {
      list.clear();
    }
  }

  @Override
  public int getSize() {
    return list.size();
  }

  @Override
  public boolean addAll(Collection<? extends Serializable> items) {
    synchronized (list) {
      boolean result = list.addAll(items);
      list.notifyAll();
      return result;
    }
  }

  private void checkInterrupted() throws InterruptedException {
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
  }
}
