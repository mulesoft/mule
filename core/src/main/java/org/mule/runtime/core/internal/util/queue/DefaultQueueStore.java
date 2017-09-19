/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.queue;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.util.queue.DefaultQueueConfiguration;
import org.mule.runtime.core.api.util.queue.QueueConfiguration;

import java.io.Serializable;

import org.apache.commons.lang3.NotImplementedException;

/**
 * Internal queue implementation that will execute operations directly to the queue storage. Stores information about a Queue
 */
public class DefaultQueueStore implements RecoverableQueueStore {

  private QueueConfiguration config;
  private String name;
  private QueueStoreDelegate delegate;
  private MuleContext muleContext;

  public DefaultQueueStore(String name, MuleContext muleContext, QueueConfiguration config) {
    this.name = name;
    this.muleContext = muleContext;
    setConfigAndDelegate(config);
  }

  public void setConfig(QueueConfiguration config) {
    setConfigAndDelegate(config);
  }

  private void setConfigAndDelegate(QueueConfiguration newConfig) {
    if (delegate != null) {
      return;
    }
    this.config = newConfig;
    if (this.config == null) {
      this.config = new DefaultQueueConfiguration();
    }
    if (this.config.isPersistent()) {
      delegate = new DualRandomAccessFileQueueStoreDelegate(this.name, muleContext.getConfiguration().getWorkingDirectory(),
                                                            muleContext, this.config.getCapacity());
    } else {
      delegate = new DefaultQueueStoreDelegate(this.config.getCapacity());
    }
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof DefaultQueueStore && name.equals(((DefaultQueueStore) obj).name));
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

  public boolean offer(Serializable o, int room, long timeout) throws InterruptedException {
    return delegate.offer(o, room, timeout);
  }

  public Serializable poll(long timeout) throws InterruptedException {
    return delegate.poll(timeout);
  }

  public Serializable peek() throws InterruptedException {
    return delegate.peek();
  }

  public void untake(Serializable item) throws InterruptedException {
    delegate.untake(item);
  }

  public int getSize() {
    return delegate.getSize();
  }

  public void clear() throws InterruptedException {
    this.delegate.clear();
  }

  public void dispose() {
    this.delegate.dispose();
  }

  public int getCapacity() {
    return config == null ? null : config.getCapacity();
  }

  public void remove(Serializable value) {
    if (this.delegate instanceof TransactionalQueueStoreDelegate) {
      ((TransactionalQueueStoreDelegate) delegate).remove(value);
    } else {
      throw new NotImplementedException(String.format("Queue of type %s does not support remove",
                                                      delegate.getClass().getCanonicalName()));
    }
  }

  public boolean contains(Serializable value) {
    if (this.delegate instanceof TransactionalQueueStoreDelegate) {
      return ((TransactionalQueueStoreDelegate) delegate).contains(value);
    } else {
      throw new NotImplementedException(String.format("Queue of type %s does not support contains",
                                                      delegate.getClass().getCanonicalName()));
    }
  }

  public void close() {
    if (this.delegate instanceof TransactionalQueueStoreDelegate) {
      ((TransactionalQueueStoreDelegate) delegate).close();
    } else {
      throw new NotImplementedException(String.format("Queue of type %s does not support close",
                                                      delegate.getClass().getCanonicalName()));
    }
  }

  @Override
  public boolean isPersistent() {
    return config.isPersistent();
  }
}
