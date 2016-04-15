/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.executor;

import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.module.db.internal.domain.query.BulkQuery;
import org.mule.module.db.internal.domain.query.Query;
import org.mule.module.db.internal.domain.query.QueryParamValue;

import java.sql.SQLException;
import java.util.List;

/**
 * Executes bulk queries against a database
 */
public interface BulkExecutor
{

    /**
     * Executes a bulk query
     *
     * @param connection connection to the database where the query will be executed. Non null
     * @param bulkQuery contains a group of non parameterized queries to execute
     * @return a non null result of the query execution
     * @throws SQLException if a database access error occurs or this method is called on a closed connection
     */
    Object execute(DbConnection connection, BulkQuery bulkQuery) throws SQLException;

    /**
     * Executes a parameterized query using a bulk of parameter sets
     *
     * @param connection connection to the database where the query will be executed. Non null
     * @param query parameterized query to executed using the sets of parameters
     * @param paramValues parameters to use to execute the query
     * @return a non null result of the query execution
     * @throws SQLException if a database access error occurs or this method is called on a closed connection
     */
    Object execute(DbConnection connection, Query query, List<List<QueryParamValue>> paramValues) throws SQLException;
}
