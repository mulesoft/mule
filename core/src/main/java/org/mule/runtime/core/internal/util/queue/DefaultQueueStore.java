/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.queue;

import org.mule.runtime.api.serialization.SerializationProtocol;
import org.mule.runtime.core.api.util.queue.DefaultQueueConfiguration;
import org.mule.runtime.core.api.util.queue.QueueConfiguration;

import java.io.Serializable;

import org.apache.commons.lang3.NotImplementedException;

/**
 * Internal queue implementation that will execute operations directly to the queue storage. Stores information about a Queue
 */
public class DefaultQueueStore implements RecoverableQueueStore {

  private QueueConfiguration config;
  private final String name;
  private QueueStoreDelegate delegate;
  private final String workingDirectory;
  private final SerializationProtocol serializer;

  public DefaultQueueStore(String name, String workingDirectory, SerializationProtocol serializer, QueueConfiguration config) {
    this.name = name;
    this.workingDirectory = workingDirectory;
    this.serializer = serializer;
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
      delegate = new DualRandomAccessFileQueueStoreDelegate(this.name, workingDirectory,
                                                            serializer, this.config.getCapacity());
    } else {
      delegate = new DefaultQueueStoreDelegate(this.config.getCapacity());
    }
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof DefaultQueueStore && name.equals(((DefaultQueueStore) obj).name));
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public void putNow(Serializable o) {
    delegate.putNow(o);
  }

  @Override
  public boolean offer(Serializable o, int room, long timeout) throws InterruptedException {
    return delegate.offer(o, room, timeout);
  }

  @Override
  public Serializable poll(long timeout) throws InterruptedException {
    return delegate.poll(timeout);
  }

  @Override
  public Serializable peek() throws InterruptedException {
    return delegate.peek();
  }

  @Override
  public void untake(Serializable item) throws InterruptedException {
    delegate.untake(item);
  }

  @Override
  public int getSize() {
    return delegate.getSize();
  }

  @Override
  public void clear() throws InterruptedException {
    this.delegate.clear();
  }

  @Override
  public void dispose() {
    this.delegate.dispose();
  }

  @Override
  public int getCapacity() {
    return config == null ? null : config.getCapacity();
  }

  @Override
  public void remove(Serializable value) {
    if (this.delegate instanceof TransactionalQueueStoreDelegate) {
      ((TransactionalQueueStoreDelegate) delegate).remove(value);
    } else {
      throw new NotImplementedException(String.format("Queue of type %s does not support remove",
                                                      delegate.getClass().getCanonicalName()));
    }
  }

  @Override
  public boolean contains(Serializable value) {
    if (this.delegate instanceof TransactionalQueueStoreDelegate) {
      return ((TransactionalQueueStoreDelegate) delegate).contains(value);
    } else {
      throw new NotImplementedException(String.format("Queue of type %s does not support contains",
                                                      delegate.getClass().getCanonicalName()));
    }
  }

  @Override
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
