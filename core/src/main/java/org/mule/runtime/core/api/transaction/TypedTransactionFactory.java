/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.transaction;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.tx.TransactionType;

/**
 * {@link TransactionFactory} that specifies the {@link TransactionType} it handles. Implementations should be registered via SPI.
 *
 * @since 4.0
 */
@NoImplement
public interface TypedTransactionFactory extends TransactionFactory {

  /**
   * @return the {@link TransactionType} handled
   */
  TransactionType getType();

}
