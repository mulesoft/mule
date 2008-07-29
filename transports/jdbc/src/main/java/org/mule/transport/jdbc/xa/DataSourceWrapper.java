/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jdbc.xa;

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

    /**
     * @deprecated this method is only kept for API compatability with 
     * JdbcTransactionalXaFunctionalTestCase. Remove if that test is refactored.
     */
    public DataSourceWrapper(XADataSource xaDataSource, TransactionManager tm)
    {
        this(xaDataSource);
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

}
