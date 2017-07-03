/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.queue;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.transaction.xa.ResourceManagerException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;

/**
 * Transaction context for transient queues
 */
public class TransientQueueTransactionContext implements LocalQueueTransactionContext {

  private static final int CLEAR_POLL_TIMEOUT = 10;

  private Map<QueueStore, List<Serializable>> added;
  private Map<QueueStore, List<Serializable>> removed;

  public boolean offer(QueueStore queue, Serializable item, long offerTimeout) throws InterruptedException {
    initializeAdded();

    List<Serializable> queueAdded = lookupAddedQueue(queue);
    // wait for enough room
    if (queue.offer(null, queueAdded.size(), offerTimeout)) {
      queueAdded.add(item);
      return true;
    } else {
      return false;
    }
  }

  public void untake(QueueStore queue, Serializable item) throws InterruptedException {
    initializeAdded();

    List<Serializable> queueAdded = lookupAddedQueue(queue);
    queueAdded.add(item);
  }

  public void clear(QueueStore queue) throws InterruptedException {
    this.initializeRemoved();
    List<Serializable> queueRemoved = this.lookupRemovedQueue(queue);
    for (Serializable discardedItem = queue.poll(CLEAR_POLL_TIMEOUT); discardedItem != null; discardedItem =
        queue.poll(CLEAR_POLL_TIMEOUT)) {
      queueRemoved.add(discardedItem);
    }

    if (this.added != null) {
      List<Serializable> queueAdded = this.lookupAddedQueue(queue);
      if (!CollectionUtils.isEmpty(queueAdded)) {
        queueRemoved.addAll(queueAdded);
        queueAdded.clear();
      }
    }

  }

  public Serializable poll(QueueStore queue, long pollTimeout) throws InterruptedException {
    Serializable value = queue.poll(pollTimeout);
    if (value != null) {
      if (removed == null) {
        removed = new HashMap<>();
      }
      List<Serializable> queueRemoved = removed.get(queue);
      if (queueRemoved == null) {
        queueRemoved = new ArrayList<>();
        removed.put(queue, queueRemoved);
      }
      queueRemoved.add(value);
    }
    return value;
  }

  public Serializable peek(QueueStore queue) throws InterruptedException {
    return queue.peek();
  }

  public int size(QueueStore queue) {
    int sz = queue.getSize();
    if (added != null) {
      List<Serializable> queueAdded = added.get(queue);
      if (queueAdded != null) {
        sz += queueAdded.size();
      }
    }
    return sz;
  }

  @Override
  public void doCommit() throws ResourceManagerException {
    try {
      if (added != null) {
        for (Map.Entry<QueueStore, List<Serializable>> entry : added.entrySet()) {
          QueueStore queue = entry.getKey();
          List<Serializable> queueAdded = entry.getValue();
          if (queueAdded != null && queueAdded.size() > 0) {
            for (Serializable object : queueAdded) {
              queue.putNow(object);
            }
          }
        }
      }
    } catch (Exception e) {
      throw new ResourceManagerException(e);
    } finally {
      added = null;
      removed = null;
    }

  }

  @Override
  public void doRollback() throws ResourceManagerException {
    if (removed != null) {
      for (Map.Entry<QueueStore, List<Serializable>> entry : removed.entrySet()) {
        QueueStore queue = entry.getKey();
        List<Serializable> queueRemoved = entry.getValue();
        if (queueRemoved != null && queueRemoved.size() > 0) {
          for (Serializable id : queueRemoved) {
            try {
              queue.putNow(id);
            } catch (InterruptedException e) {
              throw new MuleRuntimeException(e);
            }
          }
        }
      }
    }
    added = null;
    removed = null;
  }

  protected void initializeAdded() {
    if (added == null) {
      added = new HashMap<>();
    }
  }

  protected void initializeRemoved() {
    if (this.removed == null) {
      this.removed = new HashMap<>();
    }
  }

  protected List<Serializable> lookupAddedQueue(QueueStore queue) {
    List<Serializable> queueAdded = added.get(queue);
    if (queueAdded == null) {
      queueAdded = new ArrayList<>();
      added.put(queue, queueAdded);
    }
    return queueAdded;
  }

  protected List<Serializable> lookupRemovedQueue(QueueStore queue) {
    List<Serializable> queueRemoved = this.removed.get(queue);
    if (queueRemoved == null) {
      queueRemoved = new ArrayList<>();
      this.removed.put(queue, queueRemoved);
    }
    return queueRemoved;
  }
}
