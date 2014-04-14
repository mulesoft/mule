/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.xa;


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

    public DataSourceWrapper(XADataSource xaDataSource)
    {
        this.xaDataSource = xaDataSource;
    }

    @Override
    public int getLoginTimeout() throws SQLException
    {
        return xaDataSource.getLoginTimeout();
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException
    {
        xaDataSource.setLoginTimeout(seconds);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException
    {
        return xaDataSource.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException
    {
        xaDataSource.setLogWriter(out);
    }

    @Override
    public Connection getConnection() throws SQLException
    {
        return new ConnectionWrapper(xaDataSource.getXAConnection());
    }

    @Override
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
