/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util.queue;

import org.mule.runtime.core.api.transaction.xa.ResourceManagerException;

import javax.transaction.xa.XAResource;

/**
 * Session for executing operation over queues.
 */
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
