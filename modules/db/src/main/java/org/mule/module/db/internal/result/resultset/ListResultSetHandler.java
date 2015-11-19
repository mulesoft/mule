/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.result.resultset;

import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.module.db.internal.result.row.RowHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Processes a {@link ResultSet} returning a list of maps.
 * <p/>
 * The processed {@link ResultSet} is closed immediately after fetching its data.
 */
public class ListResultSetHandler implements ResultSetHandler
{

    private final RowHandler rowHandler;

    public ListResultSetHandler(RowHandler rowHandler)
    {
        this.rowHandler = rowHandler;
    }

    @Override
    public List<Map<String, Object>> processResultSet(DbConnection connection, ResultSet resultSet) throws SQLException
    {
        List<Map<String, Object>> results = new LinkedList<Map<String, Object>>();
        try
        {
            while (resultSet.next())
            {
                results.add(rowHandler.process(resultSet));
            }
        }
        finally
        {
            resultSet.close();
        }
        return results;
    }

    @Override
    public boolean requiresMultipleOpenedResults()
    {
        return false;
    }
}
