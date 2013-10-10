/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
