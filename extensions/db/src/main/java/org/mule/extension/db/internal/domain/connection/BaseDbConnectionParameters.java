/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection;

import static org.mule.extension.db.api.param.TransactionIsolation.NOT_CONFIGURED;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import org.mule.extension.db.api.param.TransactionIsolation;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

public abstract class BaseDbConnectionParameters {

  private static final String TRANSACTION_CONFIGURATION = "Transaction Configuration";

  /**
   * The transaction isolation level to set on the driver when connecting the database.
   */
  @Parameter
  @Optional(defaultValue = "NOT_CONFIGURED")
  @Expression(NOT_SUPPORTED)
  @Placement(tab = TRANSACTION_CONFIGURATION)
  private TransactionIsolation transactionIsolation = NOT_CONFIGURED;

  /**
   * Indicates whether or not the created datasource has to support XA transactions. Default is false.
   */
  @Parameter
  @Optional(defaultValue = "false")
  @Expression(NOT_SUPPORTED)
  @Placement(tab = TRANSACTION_CONFIGURATION)
  @DisplayName("Use XA Transactions")
  private boolean useXaTransactions = false;

  public TransactionIsolation getTransactionIsolation() {
    return transactionIsolation;
  }

  public boolean isUseXaTransactions() {
    return useXaTransactions;
  }
}
