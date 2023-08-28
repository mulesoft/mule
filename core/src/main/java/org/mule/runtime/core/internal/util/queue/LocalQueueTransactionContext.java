/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
