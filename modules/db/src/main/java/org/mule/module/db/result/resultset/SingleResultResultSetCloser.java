/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.result.resultset;

import org.mule.module.db.domain.connection.DbConnection;
import org.mule.module.db.result.statement.StatementStreamingResultSetCloser;

import java.sql.ResultSet;

/**
 * Closes a connection after a {@link ResultSet} has been processed
 */
public class SingleResultResultSetCloser extends StatementStreamingResultSetCloser
{

    @Override
    public void close(DbConnection connection, ResultSet resultSet)
    {
        super.close(connection, resultSet);

        connection.release();
    }
}
