/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.util.queue;

import org.mule.runtime.api.exception.MuleRuntimeException;

/**
 * Provides access to the transaction context if there is one for the current session.
 */
public interface TransactionContextProvider {

  /**
   * @return true if there is a transaction context created for the current session
   */
  boolean isTransactional();

  /**
   * @return if {@link #isTransactional()} returns true it will return the {@link QueueTransactionContext} related to the current
   *         session.
   * @throws {@link MuleRuntimeException} if {@link #isTransactional()} is false.
   */
  QueueTransactionContext getTransactionalContext();
}
