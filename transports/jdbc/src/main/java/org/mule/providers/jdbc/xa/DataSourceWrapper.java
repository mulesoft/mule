/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jdbc.xa;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.transaction.TransactionManager;

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

    public DataSourceWrapper(XADataSource xaDataSource, TransactionManager tm)
    {
        this.xaDataSource = xaDataSource;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.sql.DataSource#getLoginTimeout()
     */
    public int getLoginTimeout() throws SQLException
    {
        return xaDataSource.getLoginTimeout();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.sql.DataSource#setLoginTimeout(int)
     */
    public void setLoginTimeout(int seconds) throws SQLException
    {
        xaDataSource.setLoginTimeout(seconds);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.sql.DataSource#getLogWriter()
     */
    public PrintWriter getLogWriter() throws SQLException
    {
        return xaDataSource.getLogWriter();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.sql.DataSource#setLogWriter(java.io.PrintWriter)
     */
    public void setLogWriter(PrintWriter out) throws SQLException
    {
        xaDataSource.setLogWriter(out);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.sql.DataSource#getConnection()
     */
    public Connection getConnection() throws SQLException
    {
        final Connection connWrapper = new ConnectionWrapper(xaDataSource.getXAConnection());
        connWrapper.setAutoCommit(false);
        return connWrapper;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.sql.DataSource#getConnection(java.lang.String, java.lang.String)
     */
    public Connection getConnection(String username, String password) throws SQLException
    {
        final Connection connWrapper = new ConnectionWrapper(xaDataSource.getXAConnection(username, password));
        connWrapper.setAutoCommit(false);
        return connWrapper;
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
}
