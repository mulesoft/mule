/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.queue;

import java.io.Serializable;

/**
 * Wrapper for QueueStore so the cache in {@link AbstractQueueManager} gets cleaned up when a
 * queue is not longer used.
 */
public class CacheAwareQueueStore implements QueueStore {

  private final QueueStoreCacheListener queueStoreCacheListener;
  private final QueueStore queueStore;

  public CacheAwareQueueStore(QueueStore queueStore, QueueStoreCacheListener queueStoreCacheListener) {
    this.queueStore = queueStore;
    this.queueStoreCacheListener = queueStoreCacheListener;
  }

  @Override
  public String getName() {
    return queueStore.getName();
  }

  @Override
  public void putNow(Serializable o) throws InterruptedException {
    queueStore.putNow(o);
  }

  @Override
  public boolean offer(Serializable o, int room, long timeout) throws InterruptedException {
    return queueStore.offer(o, room, timeout);
  }

  @Override
  public Serializable poll(long timeout) throws InterruptedException {
    return queueStore.poll(timeout);
  }

  @Override
  public Serializable peek() throws InterruptedException {
    return queueStore.peek();
  }

  @Override
  public void untake(Serializable item) throws InterruptedException {
    queueStore.untake(item);
  }

  @Override
  public int getSize() {
    return queueStore.getSize();
  }

  @Override
  public void clear() throws InterruptedException {
    queueStore.clear();
  }

  @Override
  public void dispose() {
    queueStore.dispose();
    queueStoreCacheListener.disposeQueueStore(queueStore);
  }

  @Override
  public int getCapacity() {
    return queueStore.getCapacity();
  }

  @Override
  public void close() {
    queueStore.close();
    queueStoreCacheListener.closeQueueStore(queueStore);
  }

  @Override
  public boolean isPersistent() {
    return queueStore.isPersistent();
  }

  QueueStore getDelegate() {
    return queueStore;
  }
}
