/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.result.resultset;

import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.module.db.internal.result.row.RowHandler;
import org.mule.module.db.internal.result.statement.StatementStreamingResultSetCloser;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Processes a {@link ResultSet} returning an iterator of maps.
 * <p/>
 * The {@link ResultSet} backing the returned {@link ResultSetIterator} will be closed when the connection it came from
 * is closed.
 */
public class IteratorResultSetHandler implements ResultSetHandler
{

    private final RowHandler rowHandler;
    private final StatementStreamingResultSetCloser streamingResultSetCloser;

    public IteratorResultSetHandler(RowHandler rowHandler, StatementStreamingResultSetCloser streamingResultSetCloser)
    {
        this.rowHandler = rowHandler;
        this.streamingResultSetCloser = streamingResultSetCloser;
    }

    @Override
    public ResultSetIterator processResultSet(DbConnection connection, ResultSet resultSet) throws SQLException
    {
        streamingResultSetCloser.trackResultSet(connection, resultSet);

        return new ResultSetIterator(connection, resultSet, rowHandler, streamingResultSetCloser);
    }

    @Override
    public boolean requiresMultipleOpenedResults()
    {
        return true;
    }
}
