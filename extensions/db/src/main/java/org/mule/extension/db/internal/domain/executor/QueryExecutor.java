/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.executor;

import org.mule.extension.db.internal.domain.autogeneratedkey.AutoGeneratedKeyStrategy;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.domain.query.Query;

import java.sql.SQLException;

/**
 * Executes queries against a database
 */
public interface QueryExecutor {

  /**
   * Executes a query
   *
   * @param connection connection to the database where the query will be executed. Non null
   * @param query query to execute. Non null
   * @return a non null result of the query execution
   * @throws SQLException if a database access error occurs or this method is called on a closed connection
   */
  Object execute(DbConnection connection, Query query) throws SQLException;

  /**
   *
   * @param connection connection to the database where the query will be executed. Non null
   * @param query query to execute. Non null
   * @param autoGeneratedKeyStrategy strategy used to process auto generated keys. Non null
   * @return a non null result of the query execution
   * @throws SQLException if a database access error occurs or this method is called on a closed connection or
   * there is an error processing auto generated keys
   */
  Object execute(DbConnection connection, Query query, AutoGeneratedKeyStrategy autoGeneratedKeyStrategy) throws SQLException;
}
