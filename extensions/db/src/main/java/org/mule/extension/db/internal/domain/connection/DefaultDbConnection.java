/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.connection;

import org.mule.extension.db.api.exception.connection.ConnectionClosingException;
import org.mule.extension.db.internal.domain.type.DbType;
import org.mule.extension.db.internal.result.resultset.ResultSetHandler;
import org.mule.extension.db.internal.result.statement.GenericStatementResultIteratorFactory;
import org.mule.extension.db.internal.result.statement.StatementResultIteratorFactory;

import com.google.common.collect.ImmutableList;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultDbConnection implements DbConnection {

  private final Connection jdbcConnection;
  private AtomicInteger streamsCount = new AtomicInteger(0);
  private final List<DbType> customDataTypes;

  public DefaultDbConnection(Connection jdbcConnection, List<DbType> customDataTypes) {
    this.jdbcConnection = jdbcConnection;
    this.customDataTypes = customDataTypes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StatementResultIteratorFactory getStatementResultIteratorFactory(ResultSetHandler resultSetHandler) {
    return new GenericStatementResultIteratorFactory(resultSetHandler);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<DbType> getVendorDataTypes() {
    return ImmutableList.of();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connection getJdbcConnection() {
    return jdbcConnection;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<DbType> getCustomDataTypes() {
    return customDataTypes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void begin() throws Exception {
    if (jdbcConnection.getAutoCommit()) {
      jdbcConnection.setAutoCommit(false);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void commit() throws SQLException {
    jdbcConnection.commit();
    abortStreaming();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void rollback() throws SQLException {
    jdbcConnection.rollback();
    abortStreaming();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void release() {
    if (isStreaming()) {
      return;
    }
    try {
      jdbcConnection.close();
    } catch (SQLException e) {
      throw new ConnectionClosingException(e);
    }
  }

  @Override
  public void beginStreaming() {
    streamsCount.incrementAndGet();
  }

  @Override
  public boolean isStreaming() {
    return streamsCount.get() > 0;
  }

  @Override
  public void endStreaming() {
    streamsCount.decrementAndGet();
  }

  private void abortStreaming() {
    streamsCount.set(0);
  }
}
