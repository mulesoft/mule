/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
