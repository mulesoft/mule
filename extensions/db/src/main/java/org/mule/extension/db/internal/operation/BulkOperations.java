/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.operation;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.mule.extension.db.internal.domain.query.QueryType.DELETE;
import static org.mule.extension.db.internal.domain.query.QueryType.INSERT;
import static org.mule.extension.db.internal.domain.query.QueryType.UPDATE;
import org.mule.extension.db.api.param.BulkQueryDefinition;
import org.mule.extension.db.api.param.BulkScript;
import org.mule.extension.db.internal.DbConnector;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.domain.executor.BulkUpdateExecutor;
import org.mule.extension.db.internal.domain.query.BulkQuery;
import org.mule.extension.db.internal.domain.query.Query;
import org.mule.extension.db.internal.domain.query.QueryParamValue;
import org.mule.extension.db.internal.domain.query.QueryType;
import org.mule.extension.db.internal.parser.QueryTemplateParser;
import org.mule.extension.db.internal.parser.SimpleQueryTemplateParser;
import org.mule.extension.db.internal.resolver.query.BulkQueryFactory;
import org.mule.extension.db.internal.resolver.query.BulkQueryResolver;
import org.mule.extension.db.internal.resolver.query.DefaultBulkQueryFactory;
import org.mule.extension.db.internal.resolver.query.FileBulkQueryFactory;
import org.mule.extension.db.internal.util.DefaultFileReader;
import org.mule.runtime.extension.api.annotation.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.UseConfig;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Contains a set of operations for performing bulk
 * DML operations from a single statement.
 *
 * @since 4.0
 */
public class BulkOperations extends BaseDbOperations {

  private BulkQueryResolver bulkQueryResolver = new BulkQueryResolver();

  /**
   * Allows executing one insert statement various times using different parameter
   * bindings. This happens using one single Database statement, which has performance
   * advantages compared to executing one single update operation various times.
   *
   * @param query           a {@link BulkQueryDefinition} as a parameter group
   * @param parameterValues A {@link List} of {@link Map}s in which every list item represents a row
   *                        to be inserted, and the map contains the parameter names as keys and
   *                        the value the parameter is bound to.
   * @param connector       the acting connector
   * @param connection      the acting connection
   * @return an array of update counts containing one element for each executed command. The elements
   * of the array are ordered according to the order in which commands were added to the batch.
   * @throws SQLException if an error is produced
   */
  public int[] bulkInsert(@ParameterGroup BulkQueryDefinition query,
                          List<Map<String, Object>> parameterValues,
                          @UseConfig DbConnector connector,
                          @Connection DbConnection connection)
      throws SQLException {

    return singleQueryBulk(query, parameterValues, INSERT, connector, connection);
  }

  /**
   * Allows executing one update statement various times using different parameter
   * bindings. This happens using one single Database statement, which has performance
   * advantages compared to executing one single update operation various times.
   *
   * @param query           a {@link BulkQueryDefinition} as a parameter group
   * @param parameterValues A {@link List} of {@link Map}s in which every list item represents a row
   *                        to be inserted, and the map contains the parameter names as keys and
   *                        the value the parameter is bound to.
   * @param connector       the acting connector
   * @param connection      the acting connection
   * @return an array of update counts containing one element for each executed command. The elements
   * of the array are ordered according to the order in which commands were added to the batch.
   * @throws SQLException if an error is produced
   */
  public int[] bulkUpdate(@ParameterGroup BulkQueryDefinition query,
                          List<Map<String, Object>> parameterValues,
                          @UseConfig DbConnector connector,
                          @Connection DbConnection connection)
      throws SQLException {

    return singleQueryBulk(query, parameterValues, UPDATE, connector, connection);
  }

  /**
   * Allows executing one delete statement various times using different parameter
   * bindings. This happens using one single Database statement, which has performance
   * advantages compared to executing one single delete operation various times.
   *
   * @param query           a {@link BulkQueryDefinition} as a parameter group
   * @param parameterValues A {@link List} of {@link Map}s in which every list item represents a row
   *                        to be inserted, and the map contains the parameter names as keys and
   *                        the value the parameter is bound to.
   * @param connector       the acting connector
   * @param connection      the acting connection
   * @return an array of update counts containing one element for each executed command. The elements
   * of the array are ordered according to the order in which commands were added to the batch.
   * @throws SQLException if an error is produced
   */
  public int[] bulkDelete(@ParameterGroup BulkQueryDefinition query,
                          List<Map<String, Object>> parameterValues,
                          @UseConfig DbConnector connector,
                          @Connection DbConnection connection)
      throws SQLException {

    return singleQueryBulk(query, parameterValues, DELETE, connector, connection);
  }

  /**
   * Executes a SQL script in one single Database statement. The script is executed as provided
   * by the user, without any parameter binding.
   *
   * @param script     a {@link BulkScript} as a parameter group
   * @param settings   a {@link QuerySettings} as a parameter group
   * @param connection the acting connection
   * @return an array of update counts containing one element for each executed command. The elements
   * of the array are ordered according to the order in which commands were added to the batch.
   * @throws SQLException if an error is produced
   */
  public int[] executeScript(@ParameterGroup BulkScript script,
                             @ParameterGroup QuerySettings settings,
                             @Connection DbConnection connection)
      throws SQLException {

    QueryTemplateParser queryParser = new SimpleQueryTemplateParser();
    BulkQueryFactory bulkQueryFactory;

    if (!isEmpty(script.getFile())) {
      bulkQueryFactory = new FileBulkQueryFactory(script.getFile(), queryParser, new DefaultFileReader());
    } else {
      bulkQueryFactory = new DefaultBulkQueryFactory(queryParser, script.getSql());
    }

    BulkQuery bulkQuery = bulkQueryFactory.resolve();

    BulkUpdateExecutor bulkUpdateExecutor =
        new BulkUpdateExecutor(getStatementFactory(null, false, settings));

    return (int[]) bulkUpdateExecutor.execute(connection, bulkQuery);
  }


  private int[] singleQueryBulk(BulkQueryDefinition query,
                                List<Map<String, Object>> values,
                                QueryType queryType,
                                DbConnector connector,
                                DbConnection connection)
      throws SQLException {

    final Query resolvedQuery = resolveQuery(query, connector, connection, queryType);

    List<List<QueryParamValue>> paramSets = resolveParamSets(values);

    BulkUpdateExecutor bulkUpdateExecutor =
        new BulkUpdateExecutor(getStatementFactory(null, false, query.getSettings()));
    return (int[]) bulkUpdateExecutor.execute(connection, resolvedQuery, paramSets);
  }

  private Query resolveQuery(BulkQueryDefinition query, DbConnector connector, DbConnection connection, QueryType... validTypes) {
    final Query resolvedQuery = bulkQueryResolver.resolve(query, connector, connection);
    validateQueryType(resolvedQuery.getQueryTemplate(), asList(validTypes));

    return resolvedQuery;
  }

  private List<List<QueryParamValue>> resolveParamSets(List<Map<String, Object>> values) {
    return values.stream().map(map -> map.entrySet().stream()
        .map(entry -> new QueryParamValue(entry.getKey(), entry.getValue()))
        .collect(toList()))
        .collect(toList());
  }
}
