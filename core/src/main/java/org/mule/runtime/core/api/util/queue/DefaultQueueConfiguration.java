/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util.queue;

public class DefaultQueueConfiguration implements QueueConfiguration {

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
