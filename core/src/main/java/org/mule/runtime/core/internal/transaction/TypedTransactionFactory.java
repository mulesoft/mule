/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transaction;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.tx.TransactionType;
import org.mule.runtime.core.privileged.transaction.TransactionFactory;

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
