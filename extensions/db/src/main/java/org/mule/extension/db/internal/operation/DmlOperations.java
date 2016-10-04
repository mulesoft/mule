/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.operation;

import static java.util.Arrays.asList;
import static org.mule.extension.db.internal.domain.query.QueryType.DELETE;
import static org.mule.extension.db.internal.domain.query.QueryType.INSERT;
import static org.mule.extension.db.internal.domain.query.QueryType.MERGE;
import static org.mule.extension.db.internal.domain.query.QueryType.SELECT;
import static org.mule.extension.db.internal.domain.query.QueryType.STORE_PROCEDURE_CALL;
import static org.mule.extension.db.internal.domain.query.QueryType.TRUNCATE;
import static org.mule.extension.db.internal.domain.query.QueryType.UPDATE;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.NOT_SUPPORTED;
import org.mule.extension.db.api.StatementResult;
import org.mule.extension.db.internal.StatementStreamingResultSetCloser;
import org.mule.extension.db.api.param.QueryDefinition;
import org.mule.extension.db.api.param.StoredProcedureCall;
import org.mule.extension.db.internal.DbConnector;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.domain.executor.SelectExecutor;
import org.mule.extension.db.internal.domain.executor.StoredProcedureExecutor;
import org.mule.extension.db.internal.domain.metadata.SelectMetadataResolver;
import org.mule.extension.db.internal.domain.metadata.StoredProcedureMetadataResolver;
import org.mule.extension.db.internal.domain.query.Query;
import org.mule.extension.db.internal.domain.query.QueryType;
import org.mule.extension.db.internal.domain.statement.QueryStatementFactory;
import org.mule.extension.db.internal.resolver.query.StoredProcedureQueryResolver;
import org.mule.extension.db.internal.result.resultset.IteratorResultSetHandler;
import org.mule.extension.db.internal.result.resultset.ListResultSetHandler;
import org.mule.extension.db.internal.result.resultset.ResultSetHandler;
import org.mule.extension.db.internal.result.row.InsensitiveMapRowHandler;
import org.mule.extension.db.internal.result.statement.EagerStatementResultHandler;
import org.mule.extension.db.internal.result.statement.StatementResultHandler;
import org.mule.extension.db.internal.result.statement.StreamingStatementResultHandler;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.ParameterGroup;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.runtime.operation.InterceptingCallback;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

/**
 * Contains a set of operations for performing single statement DML operations
 *
 * @since 4.0
 */
public class DmlOperations extends BaseDbOperations {

  @Inject
  private StatementStreamingResultSetCloser resultSetCloser;

  private final StoredProcedureQueryResolver storedProcedureResolver = new StoredProcedureQueryResolver();

  /**
   * Selects data from a database
   *
   * @param query a {@link QueryDefinition} as a parameter group
   * @param streaming if enabled retrieves the result set in blocks so that memory is not exhausted in case of large data sets.
   *        This works in tandem with the fetch size parameter
   * @param statementAttributes a {@link StatementAttributes} as a parameter group
   * @param connector the acting connector
   * @param connection the acting connection
   * @return depending on the value of {@code streaming}, it can be a {@link List} or {@link Iterator} of maps
   * @throws SQLException if an error is produced
   */
  @OutputResolver(SelectMetadataResolver.class)
  public InterceptingCallback<Object> select(@ParameterGroup QueryDefinition query,
                                             @Optional(defaultValue = "false") @Expression(NOT_SUPPORTED) boolean streaming,
                                             @ParameterGroup StatementAttributes statementAttributes,
                                             @UseConfig DbConnector connector,
                                             @Connection DbConnection connection)
      throws SQLException {

    final Query resolvedQuery = resolveQuery(query, connector, connection, SELECT, STORE_PROCEDURE_CALL);

    QueryStatementFactory statementFactory = getStatementFactory(statementAttributes, streaming, query.getSettings());
    InsensitiveMapRowHandler recordHandler = new InsensitiveMapRowHandler();
    ResultSetHandler resultSetHandler = streaming
        ? new IteratorResultSetHandler(recordHandler, resultSetCloser)
        : new ListResultSetHandler(recordHandler);

    Object result = new SelectExecutor(statementFactory, resultSetHandler).execute(connection, resolvedQuery);

    return interceptingCallback(result, connection);
  }

  /**
   * Inserts data into a Database
   *
   * @param query {@link QueryDefinition} as a parameter group
   * @param autoGeneratedKeyAttributes an {@link AutoGeneratedKeyAttributes} as a parameter group
   * @param connector the acting connector
   * @param connection the acting connection
   * @return a {@link StatementResult}
   * @throws SQLException if an error is produced
   */
  public StatementResult insert(@ParameterGroup QueryDefinition query,
                                @ParameterGroup AutoGeneratedKeyAttributes autoGeneratedKeyAttributes,
                                @UseConfig DbConnector connector,
                                @Connection DbConnection connection)
      throws SQLException {

    final Query resolvedQuery = resolveQuery(query, connector, connection, INSERT);
    return executeUpdate(query, null, autoGeneratedKeyAttributes, connection, resolvedQuery);
  }

  /**
   * Updates data in a database.
   *
   * @param query {@link QueryDefinition} as a parameter group
   * @param autoGeneratedKeyAttributes an {@link AutoGeneratedKeyAttributes} as a parameter group
   * @param connector the acting connector
   * @param connection the acting connection
   * @return a {@link StatementResult}
   * @throws SQLException if an error is produced
   */
  public StatementResult update(@ParameterGroup QueryDefinition query,
                                @ParameterGroup AutoGeneratedKeyAttributes autoGeneratedKeyAttributes,
                                @UseConfig DbConnector connector,
                                @Connection DbConnection connection)
      throws SQLException {

    final Query resolvedQuery = resolveQuery(query, connector, connection, UPDATE, TRUNCATE, MERGE, STORE_PROCEDURE_CALL);
    return executeUpdate(query, null, autoGeneratedKeyAttributes, connection, resolvedQuery);
  }

  /**
   * Deletes data in a database.
   *
   * @param query {@link QueryDefinition} as a parameter group
   * @param connector the acting connector
   * @param connection the acting connection
   * @return the number of affected rows
   * @throws SQLException if an error is produced
   */
  public int delete(@ParameterGroup QueryDefinition query, @UseConfig DbConnector connector, @Connection DbConnection connection)
      throws SQLException {

    final Query resolvedQuery = resolveQuery(query, connector, connection, DELETE);
    return executeUpdate(query, null, null, connection, resolvedQuery).getAffectedRows();
  }

  /**
   * Invokes a Stored Procedure on the database.
   *
   * @param call a {@link StoredProcedureCall} as a parameter group
   * @param streaming if enabled retrieves the result set in blocks so that memory is not exhausted in case of large data sets.
   *        This works in tandem with the fetch size parameter
   * @param statementAttributes a {@link StatementAttributes} as a parameter group
   * @param autoGeneratedKeyAttributes an {@link AutoGeneratedKeyAttributes} as a parameter group
   * @param connector the acting connector
   * @param connection the acting connection
   * @return A {@link Map} with the procedure's output
   * @throws SQLException if an error is produced
   */
  @OutputResolver(StoredProcedureMetadataResolver.class)
  public InterceptingCallback<Map<String, Object>> storedProcedure(@ParameterGroup StoredProcedureCall call,
                                                                   @Optional(
                                                                       defaultValue = "false") @Expression(NOT_SUPPORTED) boolean streaming,
                                                                   @ParameterGroup StatementAttributes statementAttributes,
                                                                   @ParameterGroup AutoGeneratedKeyAttributes autoGeneratedKeyAttributes,
                                                                   @UseConfig DbConnector connector,
                                                                   @Connection DbConnection connection)
      throws SQLException {

    final Query resolvedQuery = resolveQuery(call, connector, connection, STORE_PROCEDURE_CALL);

    QueryStatementFactory statementFactory = getStatementFactory(statementAttributes, streaming, call.getSettings());

    InsensitiveMapRowHandler recordHandler = new InsensitiveMapRowHandler();

    StatementResultHandler resultHandler = streaming
        ? new StreamingStatementResultHandler(new IteratorResultSetHandler(recordHandler, resultSetCloser))
        : new EagerStatementResultHandler(new ListResultSetHandler(recordHandler));

    Map<String, Object> result = (Map<String, Object>) new StoredProcedureExecutor(statementFactory, resultHandler)
        .execute(connection, resolvedQuery, getAutoGeneratedKeysStrategy(autoGeneratedKeyAttributes));

    return interceptingCallback(result, connection);
  }


  private <T> InterceptingCallback<T> interceptingCallback(T result, DbConnection connection) {
    return new InterceptingCallback<T>() {

      @Override
      public T getResult() throws Exception {
        return result;
      }

      @Override
      public void onException(Exception exception) {
        resultSetCloser.closeResultSets(connection);
      }
    };
  }

  protected Query resolveQuery(StoredProcedureCall call,
                               DbConnector connector,
                               DbConnection connection,
                               QueryType... validTypes) {
    final Query resolvedQuery = storedProcedureResolver.resolve(call, connector, connection);
    validateQueryType(resolvedQuery.getQueryTemplate(), asList(validTypes));

    return resolvedQuery;
  }
}
