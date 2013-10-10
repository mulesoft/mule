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

    /**
     * @param xads The XADataSource to set.
     */
    public void setXaDataSource(XADataSource xads)
    {
        this.xaDataSource = xads;
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
