/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.util.queue;

public final class DefaultQueueConfiguration implements QueueConfiguration {

  private final boolean persistent;
  private final int capacity;

  public DefaultQueueConfiguration() {
    this(QueueConfiguration.MAXIMUM_CAPACITY, false);
  }

  public DefaultQueueConfiguration(int capacity, boolean isPersistent) {
    this.capacity = capacity;
    this.persistent = isPersistent;
  }

  @Override
  public boolean isPersistent() {
    return persistent;
  }

  @Override
  public int getCapacity() {
    return capacity;
  }

  @Override
  public String toString() {
    return String.format("DefaultQueueConfiguration{" + "persistent=%s, capacity=%s}", persistent, capacity);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (!obj.getClass().equals(this.getClass())) {
      return false;
    }
    return persistent == ((DefaultQueueConfiguration) obj).persistent && capacity == ((DefaultQueueConfiguration) obj).capacity;
  }
}
