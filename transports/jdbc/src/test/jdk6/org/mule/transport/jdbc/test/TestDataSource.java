/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jdbc.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

public class TestDataSource implements DataSource
{
    @Override
    public Connection getConnection() throws SQLException
    {
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.getAutoCommit()).thenReturn(false);
        return mockConnection;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException
    {
        return getConnection();
    }

    @Override
    public int getLoginTimeout() throws SQLException
    {
        return 0;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException
    {
        return null;
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException
    {
        // nop
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException
    {
        // nop
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException
    {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException
    {
        return null;
    }
}


