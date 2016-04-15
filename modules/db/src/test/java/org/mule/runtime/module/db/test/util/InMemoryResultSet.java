/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.test.util;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

/**
 * In memory implementation of {@link java.sql.ResultSet} for testing purpose.
 */
public class InMemoryResultSet extends AbstractInMemoryResultSet
{

    /**
     * Creates a new in memory resultSet
     *
     * @param columns column definitions
     * @param records values for each record using the order defined in the columns
     */
    public InMemoryResultSet(List<ColumnMetadata> columns, List<Map<String, Object>> records, Statement statement)
    {
        super(columns, records, statement);
    }

    @Override
    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }
}
