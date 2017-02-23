/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.executor;

import org.mule.extension.db.internal.domain.autogeneratedkey.AutoGenerateKeysStrategy;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.domain.logger.SingleQueryLogger;
import org.mule.extension.db.internal.domain.query.Query;
import org.mule.extension.db.internal.domain.statement.StatementFactory;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Base class for executors that execute a single query
 */
public abstract class AbstractSingleQueryExecutor extends AbstractExecutor implements QueryExecutor {

  public AbstractSingleQueryExecutor(StatementFactory statementFactory) {
    super(statementFactory);
  }

  @Override
  public final Object execute(DbConnection connection, Query query) throws SQLException {
    Statement statement = statementFactory.create(connection, query.getQueryTemplate());
    return safely(statement, () -> {
      prepareQuery(statement, query);
      return doExecuteQuery(connection, statement, query);
    });
  }

  @Override
  public final Object execute(DbConnection connection, Query query, AutoGenerateKeysStrategy autoGenerateKeysStrategy)
      throws SQLException {
    Statement statement = statementFactory.create(connection, query.getQueryTemplate(), autoGenerateKeysStrategy);

    return safely(statement, () -> {
      prepareQuery(statement, query);
      return doExecuteQuery(connection, statement, query, autoGenerateKeysStrategy);
    });
  }

  protected abstract Object doExecuteQuery(DbConnection connection, Statement statement, Query query) throws SQLException;

  protected abstract Object doExecuteQuery(DbConnection dbConnection, Statement statement, Query query,
                                           AutoGenerateKeysStrategy autoGenerateKeysStrategy)
      throws SQLException;

  protected void prepareQuery(Statement statement, Query query) throws SQLException {
    SingleQueryLogger queryLogger = queryLoggerFactory.createQueryLogger(LOGGER, query.getQueryTemplate());

    if (statement instanceof PreparedStatement) {
      doProcessParameters((PreparedStatement) statement, query.getQueryTemplate(), query.getParamValues(), queryLogger);
    }

    queryLogger.logQuery();
  }

  private Object safely(Statement statement, SQLDelegate delegate) throws SQLException {
    try {
      return delegate.run();
    } catch (SQLException e) {
      try {
        statement.close();
      } catch (SQLException e2) {
        if (LOGGER.isWarnEnabled()) {
          LOGGER.warn("Could not close statement", e2);
        }
      }
      throw e;
    }
  }

  @FunctionalInterface
  private interface SQLDelegate {

    Object run() throws SQLException;
  }
}
