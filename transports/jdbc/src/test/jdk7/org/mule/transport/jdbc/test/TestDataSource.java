/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jdbc.test;

import com.mockobjects.dynamic.Mock;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

public class TestDataSource implements DataSource
{
    public Connection getConnection() throws SQLException
    {
        Mock mockConnection = new Mock(Connection.class);
        mockConnection.expectAndReturn("getAutoCommit", false);
        mockConnection.expect("commit");
        mockConnection.expect("close");

        return (Connection) mockConnection.proxy();
    }

    public Connection getConnection(String username, String password) throws SQLException
    {
        return getConnection();
    }

    public int getLoginTimeout() throws SQLException
    {
        return 0;
    }

    public PrintWriter getLogWriter() throws SQLException
    {
        return null;
    }

    public void setLoginTimeout(int seconds) throws SQLException
    {
        // nop
    }

    public void setLogWriter(PrintWriter out) throws SQLException
    {
        // nop
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException
    {
        return false;
    }

    public <T> T unwrap(Class<T> iface) throws SQLException
    {
        return null;
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException
    {
        return null;
    }
}


