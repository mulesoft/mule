/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
