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
import static org.mule.extension.db.internal.operation.AutoGenerateKeysAttributes.AUTO_GENERATE_KEYS;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;
import org.mule.extension.db.api.StatementResult;
import org.mule.extension.db.api.param.QueryDefinition;
import org.mule.extension.db.api.param.StoredProcedureCall;
import org.mule.extension.db.internal.DbConnector;
import org.mule.extension.db.internal.StatementStreamingResultSetCloser;
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
import org.mule.extension.db.internal.result.resultset.ResultSetHandler;
import org.mule.extension.db.internal.result.resultset.ResultSetIterator;
import org.mule.extension.db.internal.result.row.InsensitiveMapRowHandler;
import org.mule.extension.db.internal.result.statement.StatementResultHandler;
import org.mule.extension.db.internal.result.statement.StreamingStatementResultHandler;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.runtime.operation.FlowListener;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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
   * Selects data from a database.
   *
   * Streaming is automatically applied to avoid preemptive consumption of such results, which may lead
   * to performance and memory issues.
   *
   * @param query     a {@link QueryDefinition} as a parameter group
   * @param connector the acting connector
   * @return depending on the value of {@code streaming}, it can be a {@link List} or {@link Iterator} of maps
   * @throws SQLException if an error is produced
   */
  @OutputResolver(output = SelectMetadataResolver.class)
  public PagingProvider<DbConnection, Map<String, Object>> select(
                                                                  @ParameterGroup(name = QUERY_GROUP) @Placement(
                                                                      tab = ADVANCED_TAB) QueryDefinition query,
                                                                  @Config DbConnector connector,
                                                                  @Connection DbConnection connection)
      throws SQLException {

    return new PagingProvider<DbConnection, Map<String, Object>>() {

      private ResultSetIterator iterator;
      private final AtomicBoolean initialised = new AtomicBoolean(false);

      @Override
      public List<Map<String, Object>> getPage(DbConnection connection) {
        ResultSetIterator iterator = getIterator(connection);
        final int fetchSize = getFetchSize(query);
        final List<Map<String, Object>> page = new ArrayList<>(fetchSize);
        for (int i = 0; i < fetchSize && iterator.hasNext(); i++) {
          page.add(iterator.next());
        }

        return page;
      }

      @Override
      public java.util.Optional<Integer> getTotalResults(DbConnection connection) {
        return java.util.Optional.empty();
      }

      @Override
      public void close() throws IOException {
        resultSetCloser.closeResultSets(connection);
      }

      private ResultSetIterator getIterator(DbConnection connection) {
        if (initialised.compareAndSet(false, true)) {
          final Query resolvedQuery = resolveQuery(query, connector, connection, SELECT, STORE_PROCEDURE_CALL);

          QueryStatementFactory statementFactory = getStatementFactory(query);
          InsensitiveMapRowHandler recordHandler = new InsensitiveMapRowHandler();
          ResultSetHandler resultSetHandler = new IteratorResultSetHandler(recordHandler, resultSetCloser);

          try {
            iterator =
                (ResultSetIterator) new SelectExecutor(statementFactory, resultSetHandler).execute(connection, resolvedQuery);
          } catch (SQLException e) {
            throw new MuleRuntimeException(e);
          }
        }

        return iterator;
      }

      @Override
      public boolean useStickyConnections() {
        return true;
      }
    };
  }

  /**
   * Inserts data into a Database
   *
   * @param query                      {@link QueryDefinition} as a parameter group
   * @param autoGenerateKeysAttributes an {@link AutoGenerateKeysAttributes} as a parameter group
   * @param connector                  the acting connector
   * @param connection                 the acting connection
   * @return a {@link StatementResult}
   * @throws SQLException if an error is produced
   */
  public StatementResult insert(@ParameterGroup(name = QUERY_GROUP) @Placement(tab = ADVANCED_TAB) QueryDefinition query,
                                @ParameterGroup(name = AUTO_GENERATE_KEYS) AutoGenerateKeysAttributes autoGenerateKeysAttributes,
                                @Config DbConnector connector,
                                @Connection DbConnection connection)
      throws SQLException {

    final Query resolvedQuery = resolveQuery(query, connector, connection, INSERT);
    return executeUpdate(query, autoGenerateKeysAttributes, connection, resolvedQuery);
  }

  /**
   * Updates data in a database.
   *
   * @param query                      {@link QueryDefinition} as a parameter group
   * @param autoGenerateKeysAttributes an {@link AutoGenerateKeysAttributes} as a parameter group
   * @param connector                  the acting connector
   * @param connection                 the acting connection
   * @return a {@link StatementResult}
   * @throws SQLException if an error is produced
   */
  public StatementResult update(@ParameterGroup(name = QUERY_GROUP) QueryDefinition query,
                                @ParameterGroup(name = AUTO_GENERATE_KEYS) AutoGenerateKeysAttributes autoGenerateKeysAttributes,
                                @Config DbConnector connector,
                                @Connection DbConnection connection)
      throws SQLException {

    final Query resolvedQuery = resolveQuery(query, connector, connection, UPDATE, TRUNCATE, MERGE, STORE_PROCEDURE_CALL);
    return executeUpdate(query, autoGenerateKeysAttributes, connection, resolvedQuery);
  }

  /**
   * Deletes data in a database.
   *
   * @param query      {@link QueryDefinition} as a parameter group
   * @param connector  the acting connector
   * @param connection the acting connection
   * @return the number of affected rows
   * @throws SQLException if an error is produced
   */
  public int delete(@ParameterGroup(name = QUERY_GROUP) QueryDefinition query,
                    @Config DbConnector connector,
                    @Connection DbConnection connection)
      throws SQLException {

    final Query resolvedQuery = resolveQuery(query, connector, connection, DELETE);
    return executeUpdate(query, null, connection, resolvedQuery).getAffectedRows();
  }

  /**
   * Invokes a Stored Procedure on the database.
   * <p>
   * When the stored procedure returns one or more {@link ResultSet} instances, streaming
   * is automatically applied to avoid preemptive consumption of such results, which may lead
   * to performance and memory issues.
   *
   * @param call       a {@link StoredProcedureCall} as a parameter group
   * @param connector  the acting connector
   * @param connection the acting connection
   * @return A {@link Map} with the procedure's output
   * @throws SQLException if an error is produced
   */
  @OutputResolver(output = StoredProcedureMetadataResolver.class)
  public Map<String, Object> storedProcedure(@ParameterGroup(name = QUERY_GROUP) StoredProcedureCall call,
                                             @ParameterGroup(
                                                 name = AUTO_GENERATE_KEYS) AutoGenerateKeysAttributes autoGenerateKeysAttributes,
                                             @Config DbConnector connector,
                                             @Connection DbConnection connection,
                                             FlowListener flowListener)
      throws SQLException {

    final Query resolvedQuery = resolveQuery(call, connector, connection, STORE_PROCEDURE_CALL);

    QueryStatementFactory statementFactory = getStatementFactory(call);

    InsensitiveMapRowHandler recordHandler = new InsensitiveMapRowHandler();

    StatementResultHandler resultHandler =
        new StreamingStatementResultHandler(new IteratorResultSetHandler(recordHandler, resultSetCloser));

    Map<String, Object> result = (Map<String, Object>) new StoredProcedureExecutor(statementFactory, resultHandler)
        .execute(connection, resolvedQuery, getAutoGeneratedKeysStrategy(autoGenerateKeysAttributes));

    flowListener.onError(e -> resultSetCloser.closeResultSets(connection));
    return result;
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
