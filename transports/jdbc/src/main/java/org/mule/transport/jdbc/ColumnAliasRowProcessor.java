/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc;

import org.mule.util.CaseInsensitiveHashMap;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;

import org.apache.commons.dbutils.BasicRowProcessor;

/**
 * Processes a row from a {@link ResultSet} using the column labels
 * instead of the column names.
 * <p/>
 * This is needed because some database drivers return different values for the
 * column name and column label. {@link BasicRowProcessor} uses column names,
 * so in the mentioned cases column aliases are lost and are only available for
 * calculated values.
 */
public class ColumnAliasRowProcessor extends BasicRowProcessor
{

    @Override
    public Map toMap(ResultSet rs) throws SQLException
    {
        Map result = new CaseInsensitiveHashMap();
        ResultSetMetaData rsmd = rs.getMetaData();
        int cols = rsmd.getColumnCount();

        for (int i = 1; i <= cols; i++)
        {
            result.put(rsmd.getColumnLabel(i), rs.getObject(i));
        }

        return result;
    }
}
