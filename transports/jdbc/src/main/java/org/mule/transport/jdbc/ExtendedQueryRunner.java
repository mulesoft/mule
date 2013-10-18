/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;

/**
 * An extended version of the Query runner that supports query timeouts
 * 
 * @since 2.2.6
 */
public class ExtendedQueryRunner extends QueryRunner
{
    private int queryTimeout;

    public ExtendedQueryRunner(DataSource ds, int queryTimeout)
    {
        super (ds);
        this.queryTimeout = queryTimeout;
    }

    @Override
    protected PreparedStatement prepareStatement(Connection conn, String sql) throws SQLException
    {
        PreparedStatement statement = super.prepareStatement(conn, sql);
        if (this.queryTimeout >= 0)
        {
            statement.setQueryTimeout(this.queryTimeout);
        }
        return statement;
    }

    public int getQueryTimeout()
    {
        return this.queryTimeout;
    }
}
