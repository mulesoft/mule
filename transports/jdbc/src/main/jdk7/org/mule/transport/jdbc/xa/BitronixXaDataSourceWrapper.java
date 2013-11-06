/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc.xa;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;

import bitronix.tm.resource.jdbc.PoolingDataSource;

/**
 * Wrapper for bitronix PoolingDataSource that will return wrapped XA connections.
 */
public class BitronixXaDataSourceWrapper implements DataSource
{

    private PoolingDataSource xaDataSource;

    public BitronixXaDataSourceWrapper(PoolingDataSource xaDataSource)
    {
        this.xaDataSource = xaDataSource;
    }

    public int getLoginTimeout() throws SQLException
    {
        return xaDataSource.getLoginTimeout();
    }

    public void setLoginTimeout(int seconds) throws SQLException
    {
        xaDataSource.setLoginTimeout(seconds);
    }

    public PrintWriter getLogWriter() throws SQLException
    {
        return xaDataSource.getLogWriter();
    }

    public void setLogWriter(PrintWriter out) throws SQLException
    {
        xaDataSource.setLogWriter(out);
    }

    public Connection getConnection() throws SQLException
    {
        return new BitronixConnectionWrapper(xaDataSource.getConnection());
    }

    public Connection getConnection(String username, String password) throws SQLException
    {
        return new BitronixConnectionWrapper(xaDataSource.getConnection(username, password));
    }

    public boolean isWrapperFor(Class<?> interfase) throws SQLException
    {
        return xaDataSource.isWrapperFor(interfase);
    }

    public <T> T unwrap(Class<T> interfase) throws SQLException
    {
        return xaDataSource.unwrap(interfase);
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException
    {
        //TODO fix once BTM-134 gets fixed.
        throw new SQLFeatureNotSupportedException("btm does not support this feature yet");
    }

}