/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transaction;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.core.api.transaction.Transaction;

import java.util.Optional;

/**
 * Adapter interface to access {@link Transaction} related functionality that we don't want exposed as part of the public API
 *
 * @since 4.2
 */
public interface TransactionAdapter extends SuspendableTransaction {

  /**
   * @return the {@link ComponentLocation} corresponding to this transaction
   */
  Optional<ComponentLocation> getComponentLocation();

  /**
   * Sets the {@link ComponentLocation} corresponding to this transaction
   *
   * @param componentLocation
   */
  void setComponentLocation(ComponentLocation componentLocation);

  /**
   * Set if the transaction should be rolled back in case of timeout, or not.
   *
   * @param rollbackIfTimeout
   */
  void setRollbackIfTimeout(boolean rollbackIfTimeout);
}
