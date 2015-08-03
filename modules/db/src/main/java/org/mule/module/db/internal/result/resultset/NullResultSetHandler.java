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
 * Returns an unprocessed {@link ResultSet}
 */
public class NullResultSetHandler implements ResultSetHandler
{

    @Override
    public Object processResultSet(DbConnection connection, ResultSet resultSet) throws SQLException
    {
        return resultSet;
    }

    @Override
    public boolean requiresMultipleOpenedResults()
    {
        return false;
    }
}
