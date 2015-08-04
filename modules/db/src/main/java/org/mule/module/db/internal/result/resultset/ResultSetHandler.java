/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.result.resultset;

import org.mule.module.db.internal.domain.connection.DbConnection;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Processes all the records from a given {@link @ResultSet} in a stateless way.
 */
public interface ResultSetHandler
{

    Object processResultSet(DbConnection connection, ResultSet resultSet) throws SQLException;

    /**
     * Indicates whether or not the resultSet handler requires that the
     * connection supports multiple opened results in order to process
     * the resultSet.
     *
     * @return {@code true} is multiple opened results is required, {@code false} otherwise
     */
    boolean requiresMultipleOpenedResults();
}
