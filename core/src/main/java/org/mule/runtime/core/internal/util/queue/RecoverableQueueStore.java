/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.queue;

import java.io.Serializable;

/**
 * Extension of {@link QueueStore} required to be able to recover a queue store base on a
 * transaction journal.
 */
public interface RecoverableQueueStore extends QueueStore {

  /**
   * Removes the value from the queue.
   *
   * @param value object to be removed
   */
  void remove(Serializable value);

  /**
   * Checks if a certain object exists in the queue.
   *
   * @param value object to search in the queue
   * @return true if contains the object, false otherwise.
   */
  boolean contains(Serializable value);

}
