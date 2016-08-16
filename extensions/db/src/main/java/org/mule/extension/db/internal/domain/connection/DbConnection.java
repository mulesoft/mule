/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.connection;

import org.mule.extension.db.internal.domain.type.DbType;
import org.mule.extension.db.internal.result.resultset.ResultSetHandler;
import org.mule.extension.db.internal.result.statement.StatementResultIterator;
import org.mule.extension.db.internal.result.statement.StatementResultIteratorFactory;
import org.mule.runtime.extension.api.connectivity.TransactionalConnection;

import java.sql.Connection;
import java.util.List;

/**
 * Wraps a {@link Connection} adding connector's specific functionality
 */
public interface DbConnection extends TransactionalConnection {

  /**
   * Returns the {@link StatementResultIteratorFactory} used to create
   * the {@link StatementResultIterator} for this connection.
   *
   * @param resultSetHandler used to process resultSets created from this connection
   * @return the {@link StatementResultIterator} for this connection.
   */
  StatementResultIteratorFactory getStatementResultIteratorFactory(ResultSetHandler resultSetHandler);

  List<DbType> getVendorDataTypes();

  Connection getJdbcConnection();

  void release();

  void beginStreaming();

  boolean isStreaming();

  void endStreaming();
}
