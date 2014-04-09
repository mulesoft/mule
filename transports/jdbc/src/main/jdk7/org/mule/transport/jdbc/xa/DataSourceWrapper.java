/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc.xa;

import org.mule.util.xa.XaResourceFactoryHolder;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;
import javax.sql.XADataSource;

/**
 * Using for unification XADataSource and DataSource
 */
public class DataSourceWrapper implements DataSource, XaResourceFactoryHolder
{
    private XADataSource xaDataSource;

    public DataSourceWrapper()
    {
        super();
    }

    public DataSourceWrapper(XADataSource xaDataSource)
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
        return new ConnectionWrapper(xaDataSource.getXAConnection());
    }

    public Connection getConnection(String username, String password) throws SQLException
    {
        return new ConnectionWrapper(xaDataSource.getXAConnection(username, password));
    }

    /**
     * @return Returns the underlying XADataSource.
     */
    public XADataSource getXaDataSource()
    {
        return xaDataSource;
    }

    /**
     * @param xads The XADataSource to set.
     */
    public void setXaDataSource(XADataSource xads)
    {
        this.xaDataSource = xads;
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
        return xaDataSource.getParentLogger();
    }

    @Override
    public Object getHoldObject()
    {
        return xaDataSource;
    }
}
