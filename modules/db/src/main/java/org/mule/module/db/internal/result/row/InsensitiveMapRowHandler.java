/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.result.row;

import org.mule.util.CaseInsensitiveHashMap;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.util.Map;

/**
 * Maps a row using returning a case insensitive map
 */
public class InsensitiveMapRowHandler implements RowHandler
{

    @Override
    public Map<String, Object> process(ResultSet resultSet) throws SQLException
    {
        CaseInsensitiveHashMap result = new CaseInsensitiveHashMap();
        ResultSetMetaData metaData = resultSet.getMetaData();
        int cols = metaData.getColumnCount();

        for (int i = 1; i <= cols; i++)
        {
            String column = metaData.getColumnLabel(i);
            Object value = resultSet.getObject(i);

            if (value instanceof SQLXML)
            {
                SQLXML sqlxml = (SQLXML) value;

                result.put(column, sqlxml.getString());
            }
            else
            {
                result.put(column, value);
            }
        }

        if (cols != result.size())
        {
            throw new IllegalArgumentException("Record cannot be mapped as it contains multiple columns with the same label. Define column aliases to solve this problem");
        }

        return result;
    }
}
