/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.util.queue;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.core.api.transaction.xa.ResourceManagerException;

import javax.transaction.xa.XAResource;

/**
 * Session for executing operation over queues.
 */
@NoImplement
public interface QueueSession extends XAResource {

  /**
   * Retrieves a queue.
   *
   * @param name identifier of the queue to retrieve
   * @return the queue
   */
  Queue getQueue(String name);

  /**
   * Starts a transaction to execute operations over a set of queue with atomicity.
   *
   * @throws ResourceManagerException
   */
  void begin() throws ResourceManagerException;

  /**
   * Commits a transaction started with {@link #begin()}
   *
   * @throws ResourceManagerException
   */
  void commit() throws ResourceManagerException;

  /**
   * Rollbacks a transaction started with {@link #begin()}
   *
   * @throws ResourceManagerException
   */
  void rollback() throws ResourceManagerException;

}
