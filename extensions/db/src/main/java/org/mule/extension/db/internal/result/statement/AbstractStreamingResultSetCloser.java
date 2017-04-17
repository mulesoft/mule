/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.result.statement;

import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.result.resultset.StreamingResultSetCloser;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for implementing {@link StreamingResultSetCloser}
 */
public class AbstractStreamingResultSetCloser implements StreamingResultSetCloser {

  protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractStreamingResultSetCloser.class);

  private final boolean autoCloseConnection;


  public AbstractStreamingResultSetCloser() {
    this(true);
  }

  public AbstractStreamingResultSetCloser(boolean autoClose) {
    this.autoCloseConnection = autoClose;
  }

  @Override
  public void close(DbConnection connection, ResultSet resultSet) {
    try {
      if (!resultSet.isClosed()) {
        resultSet.close();
      }
    } catch (SQLException e) {
      LOGGER.warn("Error attempting to close resultSet", e);
    } finally {
      connection.endStreaming();
      if (autoCloseConnection) {
        connection.release();
      }
    }
  }
}
