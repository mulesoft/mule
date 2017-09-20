/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.queue;

import java.io.Serializable;

/**
 * A marker interface for a QueueStoreDelegate that, along with its store, is transactional
 */
public interface TransactionalQueueStoreDelegate extends QueueStoreDelegate {

  /**
   * Remove the specified value from the queue.
   *
   * Used only during recovery phase which means this method does not require great performance
   *
   * @param value object to be removed
   */
  void remove(Serializable value);

  /**
   * checks if the given value exists in the queue
   *
   * Used only during recovery phase which means this method does not require great performance
   *
   * @param value object to check if exists in the queue
   */
  boolean contains(Serializable value);

  /**
   * Releases all resources managed by this queue without removing the queue data.
   */
  void close();

}
