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

  /**
   * @return A list of customer defined {@link DbType}s
   */
  List<DbType> getCustomDataTypes();

  /**
   * @return A list of {@link DbType}s which are specific to the Database vendor
   */
  List<DbType> getVendorDataTypes();

  /**
   * @return The underlying JDBC connection
   */
  Connection getJdbcConnection();

  /**
   * Closes the underlying JDBC connection, provided that {@link #isStreaming()} is
   * {@code false}
   */
  void release();

  /**
   * Starts streaming. Invoke this method when a streaming resultset generated with {@code this}
   * connection is about to be iterated
   */
  void beginStreaming();

  /**
   * @return whether {@link #beginStreaming()} has been invoked on {@code this} instance
   * but {@link #endStreaming()} has not
   */
  boolean isStreaming();

  /**
   * Marks that the streaming is over
   */
  void endStreaming();
}
