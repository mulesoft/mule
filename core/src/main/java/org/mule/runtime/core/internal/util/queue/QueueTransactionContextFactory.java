/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.util.queue;

/**
 * Factory for a transient and persistent {@link QueueTransactionContext}
 */
public interface QueueTransactionContextFactory<T extends QueueTransactionContext> {

  /**
   * @return a transaction context for persistent queues
   */
  T createPersistentTransactionContext();

  /**
   * @return a transaction context for transient queues
   */
  T createTransientTransactionContext();
}
