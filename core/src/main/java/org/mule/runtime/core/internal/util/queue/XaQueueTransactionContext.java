/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.util.queue;

import org.mule.runtime.core.api.transaction.xa.ResourceManagerException;

/**
 * XA Transaction context for queues
 */
public interface XaQueueTransactionContext extends QueueTransactionContext {

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

  /**
   * Prepares the current transaction context for a commit
   *
   * @throws ResourceManagerException
   */
  void doPrepare() throws ResourceManagerException;

}
