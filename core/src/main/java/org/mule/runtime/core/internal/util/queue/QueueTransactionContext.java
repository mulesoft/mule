/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.util.queue;

import java.io.Serializable;

/**
 * Contract for a transactional context for queue operations.
 */
public interface QueueTransactionContext {

  boolean offer(QueueStore queue, Serializable item, long offerTimeout) throws InterruptedException;

  void untake(QueueStore queue, Serializable item) throws InterruptedException;

  void clear(QueueStore queue) throws InterruptedException;

  Serializable poll(QueueStore queue, long pollTimeout) throws InterruptedException;

  Serializable peek(QueueStore queue) throws InterruptedException;

  int size(QueueStore queue);

}
