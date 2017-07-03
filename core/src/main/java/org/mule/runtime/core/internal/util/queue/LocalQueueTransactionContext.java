/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.queue;

import org.mule.runtime.core.api.transaction.xa.ResourceManagerException;

/**
 * Specialization of {@link QueueTransactionContext} for local queue transactions
 */
public interface LocalQueueTransactionContext extends QueueTransactionContext {

  /**
   * Commits the current transaction context operations
   *
   * @throws ResourceManagerException
   */
  void doCommit() throws ResourceManagerException;

  /**
   * Rollbacks the current transaction context operations
   *
   * @throws ResourceManagerException
   */
  void doRollback() throws ResourceManagerException;

}
