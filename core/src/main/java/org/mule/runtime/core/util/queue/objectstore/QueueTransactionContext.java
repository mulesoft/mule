/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.queue.objectstore;

import org.mule.runtime.core.api.store.ObjectStoreException;
import org.mule.runtime.core.util.queue.objectstore.xa.AbstractTransactionContext;
import org.mule.runtime.core.util.xa.ResourceManagerException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;

/**
 * @deprecated this class will be removed in Mule 4.0 in favor of the new queue implementation
 */
@Deprecated
public class QueueTransactionContext extends AbstractTransactionContext {

  private final TransactionalQueueManager transactionalQueueManager;
  private Map<QueueInfo, List<Serializable>> added;
  private Map<QueueInfo, List<Serializable>> removed;

  public QueueTransactionContext(TransactionalQueueManager transactionalQueueManager) {
    super();
    this.transactionalQueueManager = transactionalQueueManager;
  }

  public boolean offer(QueueInfo queue, Serializable item, long offerTimeout) throws InterruptedException, ObjectStoreException {
    readOnly = false;
    if (queue.canTakeFromStore()) {
      queue.writeToObjectStore(item);
      return true;
    }

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

  public void untake(QueueInfo queue, Serializable item) throws InterruptedException, ObjectStoreException {
    readOnly = false;
    if (queue.canTakeFromStore()) {
      queue.writeToObjectStore(item);
    }

    initializeAdded();

    List<Serializable> queueAdded = lookupAddedQueue(queue);
    queueAdded.add(item);
  }

  public void clear(QueueInfo queue) throws InterruptedException {
    this.readOnly = false;
    if (queue.canTakeFromStore()) {
      queue.clear();
    }

    this.initializeRemoved();
    List<Serializable> queueRemoved = this.lookupRemovedQueue(queue);
    for (Serializable discardedItem = queue.poll(timeout); discardedItem != null; discardedItem = queue.poll(timeout)) {
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

  public Serializable poll(QueueInfo queue, long pollTimeout) throws InterruptedException, ObjectStoreException {
    readOnly = false;
    if (added != null) {
      List<Serializable> queueAdded = added.get(queue);
      if (queueAdded != null && queueAdded.size() > 0) {
        return queueAdded.remove(queueAdded.size() - 1);
      }
    }

    if (queue.canTakeFromStore()) {
      // TODO: verify that the queue is transactional too
      return queue.takeNextItemFromStore(timeout);
    }

    Serializable key;
    Serializable value = null;
    try {
      key = queue.poll(pollTimeout);
    } catch (InterruptedException e) {
      if (!transactionalQueueManager.getMuleContext().isStopping()) {
        throw e;
      }
      // if disposing, ignore
      return null;
    }

    if (key != null) {
      if (removed == null) {
        removed = new HashMap<QueueInfo, List<Serializable>>();
      }
      List<Serializable> queueRemoved = removed.get(queue);
      if (queueRemoved == null) {
        queueRemoved = new ArrayList<Serializable>();
        removed.put(queue, queueRemoved);
      }
      value = transactionalQueueManager.doLoad(queue, key);
      if (value != null) {
        queueRemoved.add(key);
      }
    }
    return value;
  }

  public Serializable peek(QueueInfo queue) throws InterruptedException, ObjectStoreException {
    readOnly = false;
    if (added != null) {
      List<Serializable> queueAdded = added.get(queue);
      if (queueAdded != null) {
        return queueAdded.get(queueAdded.size() - 1);
      }
    }

    Serializable o = queue.peek();
    if (o != null) {
      o = transactionalQueueManager.doLoad(queue, o);
    }
    return o;
  }

  public int size(QueueInfo queue) {
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
        for (Map.Entry<QueueInfo, List<Serializable>> entry : added.entrySet()) {
          QueueInfo queue = entry.getKey();
          List<Serializable> queueAdded = entry.getValue();
          if (queueAdded != null && queueAdded.size() > 0) {
            for (Serializable object : queueAdded) {
              Serializable id = transactionalQueueManager.doStore(queue, object);
              queue.putNow(id);
            }
          }
        }
      }
      if (removed != null) {
        for (Map.Entry<QueueInfo, List<Serializable>> entry : removed.entrySet()) {
          QueueInfo queue = entry.getKey();
          List<Serializable> queueRemoved = entry.getValue();
          if (queueRemoved != null && queueRemoved.size() > 0) {
            for (Serializable id : queueRemoved) {
              transactionalQueueManager.doRemove(queue, id);
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
      for (Map.Entry<QueueInfo, List<Serializable>> entry : removed.entrySet()) {
        QueueInfo queue = entry.getKey();
        List<Serializable> queueRemoved = entry.getValue();
        if (queueRemoved != null && queueRemoved.size() > 0) {
          for (Serializable id : queueRemoved) {
            queue.putNow(id);
          }
        }
      }
    }
    added = null;
    removed = null;
  }

  protected void initializeAdded() {
    if (added == null) {
      added = new HashMap<QueueInfo, List<Serializable>>();
    }
  }

  protected void initializeRemoved() {
    if (this.removed == null) {
      this.removed = new HashMap<QueueInfo, List<Serializable>>();
    }
  }

  protected List<Serializable> lookupAddedQueue(QueueInfo queue) {
    List<Serializable> queueAdded = added.get(queue);
    if (queueAdded == null) {
      queueAdded = new ArrayList<Serializable>();
      added.put(queue, queueAdded);
    }
    return queueAdded;
  }

  protected List<Serializable> lookupRemovedQueue(QueueInfo queue) {
    List<Serializable> queueRemoved = this.removed.get(queue);
    if (queueRemoved == null) {
      queueRemoved = new ArrayList<Serializable>();
      this.removed.put(queue, queueRemoved);
    }
    return queueRemoved;
  }
}
