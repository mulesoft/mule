/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.util.queue.objectstore;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.store.ListableObjectStore;
import org.mule.runtime.core.api.store.ObjectStore;
import org.mule.runtime.core.api.store.ObjectStoreException;
import org.mule.runtime.core.api.store.QueueStore;
import org.mule.runtime.core.util.UUID;
import org.mule.runtime.core.util.queue.Queue;
import org.mule.runtime.core.util.queue.QueueManager;
import org.mule.runtime.core.util.queue.QueueSession;
import org.mule.runtime.core.util.queue.objectstore.xa.AbstractTransactionContext;
import org.mule.runtime.core.util.queue.objectstore.xa.AbstractXAResourceManager;
import org.mule.runtime.core.util.xa.ResourceManagerException;
import org.mule.runtime.core.util.xa.ResourceManagerSystemException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.transaction.xa.XAResource;

/**
 * The Transactional Queue Manager is responsible for creating and Managing transactional Queues. Queues can also be persistent by
 * setting a persistence strategy on the manager. Default straties are provided for Memory, Jounaling, Cache and File.
 *
 * @deprecated this class will be removed in Mule 4.0 in favor of the new queue implementation
 */
@Deprecated
public class TransactionalQueueManager extends AbstractXAResourceManager implements QueueManager, MuleContextAware {

  private final Map<String, QueueInfo> queues = new HashMap<String, QueueInfo>();

  private QueueConfiguration defaultQueueConfiguration;
  private MuleContext muleContext;
  private final Set<QueueStore> queueObjectStores = new HashSet<QueueStore>();
  private final Set<ListableObjectStore> listableObjectStores = new HashSet<ListableObjectStore>();
  private final ReadWriteLock queuesLock = new ReentrantReadWriteLock();

  /**
   * {@inheritDoc}
   *
   * @return an instance of {@link org.mule.runtime.core.util.queue.TransactionalQueueSession}
   */
  @Override
  public synchronized QueueSession getQueueSession() {
    return new TransactionalQueueSession(this, this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void setDefaultQueueConfiguration(org.mule.runtime.core.util.queue.QueueConfiguration config) {
    this.defaultQueueConfiguration = (QueueConfiguration) config;
    addStore(((QueueConfiguration) config).objectStore);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void setQueueConfiguration(String queueName, org.mule.runtime.core.util.queue.QueueConfiguration config) {
    getQueue(queueName, (QueueConfiguration) config).setConfig((QueueConfiguration) config);
    addStore(((QueueConfiguration) config).objectStore);
  }

  protected void disposeQueue(Queue queue) throws MuleException, InterruptedException {
    if (queue == null) {
      throw new IllegalArgumentException("Queue to be disposed cannot be null");
    }

    final String queueName = queue.getName();
    Lock lock = this.queuesLock.writeLock();
    lock.lock();
    try {
      if (!this.queues.containsKey(queueName)) {
        throw new IllegalArgumentException(String.format("There's no queue for name %s", queueName));
      }

      this.queues.remove(queueName);
    } finally {
      lock.unlock();
    }

    queue.clear();

    if (queue instanceof Stoppable) {
      ((Stoppable) queue).stop();
    }

    if (queue instanceof Disposable) {
      ((Disposable) queue).dispose();
    }
  }

  protected QueueInfo getQueue(String name) {
    return getQueue(name, defaultQueueConfiguration);
  }

  protected QueueInfo getQueue(String name, QueueConfiguration config) {
    Lock lock = this.queuesLock.writeLock();
    lock.lock();
    try {
      QueueInfo q = queues.get(name);
      if (q == null) {
        q = new QueueInfo(name, muleContext, config);
        queues.put(name, q);
      }

      return q;
    } finally {
      lock.unlock();
    }
  }

  public QueueInfo getQueueInfo(String name) {
    Lock lock = this.queuesLock.readLock();
    lock.lock();
    try {
      QueueInfo q = queues.get(name);
      return q == null ? q : new QueueInfo(q);
    } finally {
      lock.unlock();
    }
  }

  @Override
  protected void doStart() throws ResourceManagerSystemException {
    findAllListableObjectStores();
    for (ListableObjectStore store : listableObjectStores) {
      try {
        store.open();
      } catch (ObjectStoreException e) {
        throw new ResourceManagerSystemException(e);
      }
    }
  }

  @Override
  protected boolean shutdown(int mode, long timeoutMSecs) {
    // Clear queues on shutdown to avoid duplicate entries on warm restarts
    // (MULE-3678)
    Lock lock = this.queuesLock.writeLock();
    lock.lock();
    try {
      queues.clear();
    } finally {
      lock.unlock();
    }
    return super.shutdown(mode, timeoutMSecs);
  }

  @Override
  protected void recover() throws ResourceManagerSystemException {
    findAllQueueStores();
    for (QueueStore store : queueObjectStores) {
      if (!store.isPersistent()) {
        continue;
      }

      try {
        List<Serializable> keys = store.allKeys();
        for (Serializable key : keys) {
          // It may contain events that aren't part of a queue MULE-6007
          if (key instanceof QueueKey) {
            QueueKey queueKey = (QueueKey) key;
            QueueInfo queue = getQueue(queueKey.queueName);
            if (queue.isQueueTransient()) {
              queue.putNow(queueKey.id);
            }
          }
        }
      } catch (Exception e) {
        throw new ResourceManagerSystemException(e);
      }
    }
  }

  @Override
  protected AbstractTransactionContext createTransactionContext(Object session) {
    return new QueueTransactionContext(this);
  }

  @Override
  protected void doBegin(AbstractTransactionContext context) {
    // Nothing special to do
  }

  @Override
  protected int doPrepare(AbstractTransactionContext context) {
    return XAResource.XA_OK;
  }

  @Override
  protected void doCommit(AbstractTransactionContext context) throws ResourceManagerException {
    context.doCommit();
  }

  protected Serializable doStore(QueueInfo queue, Serializable object) throws ObjectStoreException {
    ObjectStore<Serializable> store = queue.getStore();

    String id = muleContext == null ? UUID.getUUID() : muleContext.getUniqueIdString();
    Serializable key = new QueueKey(queue.getName(), id);
    store.store(key, object);
    return id;
  }

  protected void doClear(QueueInfo queue) throws ObjectStoreException, InterruptedException {
    queue.clear();
  }

  protected void doRemove(QueueInfo queue, Serializable id) throws ObjectStoreException {
    ObjectStore<Serializable> store = queue.getStore();

    Serializable key = new QueueKey(queue.getName(), id);
    store.remove(key);
  }

  protected Serializable doLoad(QueueInfo queue, Serializable id) throws ObjectStoreException {
    ObjectStore<Serializable> store = queue.getStore();

    Serializable key = new QueueKey(queue.getName(), id);
    return store.retrieve(key);
  }

  @Override
  protected void doRollback(AbstractTransactionContext context) throws ResourceManagerException {
    context.doRollback();
  }

  protected void findAllListableObjectStores() {
    if (muleContext != null) {
      for (ListableObjectStore store : muleContext.getRegistry().lookupByType(ListableObjectStore.class).values()) {
        addStore(store);
      }
    }
  }

  protected synchronized void findAllQueueStores() {
    if (muleContext != null) {
      for (QueueStore store : muleContext.getRegistry().lookupByType(QueueStore.class).values()) {
        addStore(store);
      }
    }
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  public MuleContext getMuleContext() {
    return muleContext;
  }

  private void addStore(ListableObjectStore<?> store) {
    if (store instanceof QueueStore) {
      queueObjectStores.add((QueueStore) store);
    }
    listableObjectStores.add(store);
  }
}
