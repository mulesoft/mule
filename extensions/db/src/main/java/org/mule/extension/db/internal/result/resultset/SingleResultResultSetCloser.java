/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.result.resultset;

import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.result.statement.AbstractStreamingResultSetCloser;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Closes a {@link ResultSet} and related {@link Statement}
 */
public class SingleResultResultSetCloser extends AbstractStreamingResultSetCloser {


  public SingleResultResultSetCloser() {
    super();
  }

  public SingleResultResultSetCloser(boolean autoCloseConnection) {
    super(autoCloseConnection);
  }

  @Override
  public void close(DbConnection connection, ResultSet resultSet) {
    Statement statement = getStatement(resultSet);

    try {
      super.close(connection, resultSet);
    } finally {
      closeStatement(statement);
    }
  }

  protected void closeStatement(Statement statement) {
    if (statement != null) {
      try {
        statement.close();
      } catch (SQLException e) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Error closing statement. Ignored", e);
        }
      }
    }
  }

  protected Statement getStatement(ResultSet resultSet) {
    Statement statement = null;

    try {
      statement = resultSet.getStatement();
    } catch (SQLException e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Error closing statement. Ignored", e);
      }
    }

    return statement;
  }
}
