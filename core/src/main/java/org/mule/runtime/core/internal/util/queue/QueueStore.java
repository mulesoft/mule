/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.util.queue;

import java.io.Serializable;

/**
 * Internal queue interface that hold queue configuration that will execute operations directly to the queue storage. Stores
 * information about a Queue.
 */
public interface QueueStore {

  String getName();

  void putNow(Serializable o) throws InterruptedException;

  boolean offer(Serializable o, int room, long timeout) throws InterruptedException;

  Serializable poll(long timeout) throws InterruptedException;

  Serializable peek() throws InterruptedException;

  void untake(Serializable item) throws InterruptedException;

  int getSize();

  void clear() throws InterruptedException;

  void dispose();

  int getCapacity();

  void close();

  boolean isPersistent();
}
