/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.result.row;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * Processes rows from a resultSet
 */
public interface RowHandler
{

    /**
     * Process a row from a resultSet
     *
     * @param resultSet resultset containing the row to process. Non null
     * @return a map where each key represents a column name and the value is the value
     * for that column in the current row. Non null.
     * @throws SQLException if a database access error occurs or this method is called
     * on a closed result set
     */
    Map<String, Object> process(ResultSet resultSet) throws SQLException;
}
