/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api.param;

import static java.sql.Connection.TRANSACTION_NONE;
import static java.sql.Connection.TRANSACTION_READ_COMMITTED;
import static java.sql.Connection.TRANSACTION_READ_UNCOMMITTED;
import static java.sql.Connection.TRANSACTION_REPEATABLE_READ;
import static java.sql.Connection.TRANSACTION_SERIALIZABLE;

/**
 * The transaction isolation levels that can be set on
 * the JDBC driver when connecting to the database.
 *
 * @since 4.0
 */
public enum TransactionIsolation {
  NONE(TRANSACTION_NONE), READ_COMMITTED(TRANSACTION_READ_COMMITTED), READ_UNCOMMITTED(
      TRANSACTION_READ_UNCOMMITTED), REPEATABLE_READ(
          TRANSACTION_REPEATABLE_READ), SERIALIZABLE(TRANSACTION_SERIALIZABLE), NOT_CONFIGURED(-1);

  private final int code;

  TransactionIsolation(int code) {
    this.code = code;
  }

  public int getCode() {
    return code;
  }
}
