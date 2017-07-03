/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
