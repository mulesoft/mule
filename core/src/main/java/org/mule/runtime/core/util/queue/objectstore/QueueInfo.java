/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.queue.objectstore;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.store.ListableObjectStore;
import org.mule.runtime.core.api.store.ObjectStore;
import org.mule.runtime.core.api.store.ObjectStoreException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores information about a Queue
 *
 * @deprecated this class will be removed in Mule 4.0 in favor of the new queue implementation
 */
@Deprecated
public class QueueInfo {

  private QueueConfiguration config;
  private String name;
  private QueueInfoDelegate delegate;
  private MuleContext muleContext;
  private boolean delegateCanTake;

  private static Map<Class<? extends ObjectStore>, QueueInfoDelegateFactory> delegateFactories =
      new HashMap<Class<? extends ObjectStore>, QueueInfoDelegateFactory>();

  public QueueInfo(String name, MuleContext muleContext, QueueConfiguration config) {
    this.name = name;
    this.muleContext = muleContext;
    setConfigAndDelegate(config);
  }

  public QueueInfo(QueueInfo other) {
    this(other.name, other.muleContext, other.config);
  }

  public void setConfig(QueueConfiguration config) {
    setConfigAndDelegate(config);
  }

  private void setConfigAndDelegate(QueueConfiguration config) {
    boolean hadConfig = this.config != null;
    this.config = config;
    int capacity = 0;
    QueueInfoDelegateFactory factory = null;
    if (config != null) {
      capacity = config.getCapacity();
      factory = delegateFactories.get(config.objectStore.getClass());
    }
    if (delegate == null || (config != null && !hadConfig)) {
      QueueInfoDelegate newDelegate =
          factory != null ? factory.createDelegate(this, muleContext) : new DefaultQueueInfoDelegate(capacity);
      delegateCanTake = newDelegate instanceof TakingQueueStoreDelegate;
      if (delegate != null && delegate instanceof DefaultQueueInfoDelegate) {
        newDelegate.addAll(((DefaultQueueInfoDelegate) delegate).list);
      }
      delegate = newDelegate;
    }
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof QueueInfo && name.equals(((QueueInfo) obj).name));
  }

  public String getName() {
    return name;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  public void putNow(Serializable o) {
    delegate.putNow(o);
  }

  public boolean offer(Serializable o, int room, long timeout) throws InterruptedException, ObjectStoreException {
    return delegate.offer(o, room, timeout);
  }

  public Serializable poll(long timeout) throws InterruptedException {
    return delegate.poll(timeout);
  }

  public Serializable peek() throws InterruptedException {
    return delegate.peek();
  }

  public void untake(Serializable item) throws InterruptedException, ObjectStoreException {
    delegate.untake(item);
  }

  public int getSize() {
    return delegate.getSize();
  }

  public void clear() throws InterruptedException {
    this.delegate.clear();
  }

  public ListableObjectStore<Serializable> getStore() {
    return config == null ? null : config.objectStore;
  }

  public static synchronized void registerDelegateFactory(Class<? extends ObjectStore> storeType,
                                                          QueueInfoDelegateFactory factory) {
    delegateFactories.put(storeType, factory);
  }

  public int getCapacity() {
    return config == null ? null : config.capacity;
  }

  public boolean canTakeFromStore() {
    return delegateCanTake;
  }

  public Serializable takeNextItemFromStore(long timeout) throws InterruptedException {
    if (canTakeFromStore()) {
      return ((TakingQueueStoreDelegate) delegate).takeFromObjectStore(timeout);
    }

    throw new UnsupportedOperationException("Method 'takeNextItemFromStore' is not supported for queue " + name);
  }

  public void writeToObjectStore(Serializable data) throws InterruptedException, ObjectStoreException {
    if (canTakeFromStore()) {
      ((TakingQueueStoreDelegate) delegate).writeToObjectStore(data);
      return;
    }

    throw new UnsupportedOperationException("Method 'writeToObjectStore' is not supported for queue " + name);
  }

  public boolean isQueueTransient() {
    return delegate instanceof TransientQueueInfoDelegate;
  }

  public boolean isQueueTransactional() {
    return delegate instanceof TransactionalQueueStoreDelegate;
  }

  /**
   * A factory for creating object store-specific queue info delegates
   */
  public static interface QueueInfoDelegateFactory {

    /**
     * Create a delegate
     */
    QueueInfoDelegate createDelegate(QueueInfo parent, MuleContext muleContext);
  }
}
