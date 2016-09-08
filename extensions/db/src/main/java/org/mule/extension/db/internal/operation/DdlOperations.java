/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.operation;

import org.mule.extension.db.api.param.QueryDefinition;
import org.mule.extension.db.api.param.QuerySettings;
import org.mule.extension.db.internal.DbConnector;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.domain.query.Query;
import org.mule.runtime.extension.api.annotation.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Text;

import java.sql.SQLException;

import static org.mule.extension.db.api.param.DbNameConstants.SQL_QUERY_TEXT;
import static org.mule.extension.db.internal.domain.query.QueryType.DDL;

/**
 * Operations to manipulate data definitions in a relational Database
 *
 * @since 4.0
 */
public class DdlOperations extends BaseDbOperations {

  /**
   * Enables execution of DDL queries against a database.
   *
   * @param sql        The text of the SQL query to be executed
   * @param settings   Parameters to configure the query
   * @param connector  the acting connector
   * @param connection the acting connection
   * @return the number of affected rows
   */
  @DisplayName("Execute DDL")
  public int executeDdl(@DisplayName(SQL_QUERY_TEXT) @Text String sql,
                        @ParameterGroup QuerySettings settings,
                        @UseConfig DbConnector connector,
                        @Connection DbConnection connection)
      throws SQLException {

    QueryDefinition query = new QueryDefinition();
    query.setSql(sql);
    query.setSettings(settings);

    final Query resolvedQuery = resolveQuery(query, connector, connection, DDL);
    return executeUpdate(query, null, null, connection, resolvedQuery).getAffectedRows();
  }
}
