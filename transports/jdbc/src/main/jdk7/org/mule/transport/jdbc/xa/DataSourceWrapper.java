/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jdbc.xa;

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
public class DataSourceWrapper implements DataSource
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
}
