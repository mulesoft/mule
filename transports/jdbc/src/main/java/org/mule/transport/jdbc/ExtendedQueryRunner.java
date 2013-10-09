/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
