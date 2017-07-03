/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
   * @return if {@link #isTransactional()} returns true it will return the
   *         {@link QueueTransactionContext} related to the current session.
   * @throws {@link MuleRuntimeException} if {@link #isTransactional()} is false.
   */
  QueueTransactionContext getTransactionalContext();
}
