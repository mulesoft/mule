/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.test.util;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

/**
 * Base class for in memory implementation of {@link java.sql.ResultSetMetaData} for testing purpose.
 * <p/>
 * NOTE: partial implementation to cover current test scenarios
 */
public class TestResultSetMetaData implements ResultSetMetaData
{

    private final List<ColumnMetadata> columns;

    public TestResultSetMetaData(List<ColumnMetadata> columns)
    {
        this.columns = columns;
    }

    public int getColumnCount() throws SQLException
    {
        return columns.size();
    }

    public boolean isAutoIncrement(int i) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public boolean isCaseSensitive(int i) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public boolean isSearchable(int i) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public boolean isCurrency(int i) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public int isNullable(int i) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public boolean isSigned(int i) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public int getColumnDisplaySize(int i) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public String getColumnLabel(int i) throws SQLException
    {
        return columns.get(i-1).getLabel();
    }

    public String getColumnName(int i) throws SQLException
    {
        return columns.get(i-1).getName();
    }

    public String getSchemaName(int i) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public int getPrecision(int i) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public int getScale(int i) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public String getTableName(int i) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public String getCatalogName(int i) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public int getColumnType(int i) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public String getColumnTypeName(int i) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public boolean isReadOnly(int i) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public boolean isWritable(int i) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public boolean isDefinitelyWritable(int i) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public String getColumnClassName(int i) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public <T> T unwrap(Class<T> tClass) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public boolean isWrapperFor(Class<?> aClass) throws SQLException
    {
        throw new UnsupportedOperationException();
    }
}
