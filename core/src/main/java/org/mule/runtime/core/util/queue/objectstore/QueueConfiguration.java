/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.queue.objectstore;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.store.QueueStore;

import java.io.Serializable;

/**
 * @deprecated this class will be removed in Mule 4.0 in favor of the new queue implementation
 */
@Deprecated
public class QueueConfiguration implements org.mule.runtime.core.util.queue.QueueConfiguration {

  public final static int INFINITY_CAPACTY = 0;
  protected final int capacity;
  protected final QueueStore<Serializable> objectStore;

  public QueueConfiguration(MuleContext context, int capacity, QueueStore<Serializable> objectStore) {
    this.capacity = capacity;
    this.objectStore = objectStore;
  }

  public QueueConfiguration(int capacity, QueueStore<Serializable> objectStore) {
    this.capacity = capacity;
    this.objectStore = objectStore;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + capacity;
    result = prime * result + objectStore.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    QueueConfiguration other = (QueueConfiguration) obj;
    if (capacity != other.capacity) {
      return false;
    }
    if (!objectStore.equals(objectStore)) {
      return false;
    }
    return true;
  }

  public boolean isPersistent() {
    return objectStore.isPersistent();
  }

  public int getCapacity() {
    return capacity;
  }
}
