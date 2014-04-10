/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.result.resultset;

import org.mule.module.db.domain.connection.DbConnection;
import org.mule.module.db.result.row.RowHandler;
import org.mule.module.db.result.statement.StatementStreamingResultSetCloser;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Processes a {@link ResultSet} returning an iterator of maps
 */
public class IteratorResultSetHandler implements ResultSetHandler
{

    private final RowHandler rowHandler;
    private StatementStreamingResultSetCloser streamingResultSetCloser = new StatementStreamingResultSetCloser();

    public IteratorResultSetHandler(RowHandler rowHandler)
    {
        this.rowHandler = rowHandler;
    }

    @Override
    public Object processResultSet(DbConnection connection, ResultSet resultSet) throws SQLException
    {
        streamingResultSetCloser.trackResultSet(connection, resultSet);

        return new ResultSetIterator(connection, resultSet, rowHandler, streamingResultSetCloser);
    }
}
