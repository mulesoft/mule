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

import org.mule.config.i18n.CoreMessages;
import org.mule.transaction.IllegalTransactionStateException;
import org.mule.transaction.TransactionCoordination;
import org.mule.transaction.XaTransaction;
import org.mule.umo.UMOTransaction;

import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.Map;

import javax.sql.XAConnection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Using for unification XAConnection and Connection
 */
public class ConnectionWrapper implements Connection
{
    private final XAConnection xaConnection;
    private Connection connection;
    private volatile boolean enlisted = false;
    protected final transient Log logger = LogFactory.getLog(getClass());

    public ConnectionWrapper(XAConnection xaCon) throws SQLException
    {
        this.xaConnection = xaCon;
        this.connection = xaCon.getConnection();
    }

    /*
    * (non-Javadoc)
    *
    * @see java.sql.Connection#getHoldability()
    */

    public int getHoldability() throws SQLException
    {
        return connection.getHoldability();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.sql.Connection#getTransactionIsolation()
     */
    public int getTransactionIsolation() throws SQLException
    {
        return connection.getTransactionIsolation();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.sql.Connection#clearWarnings()
     */
    public void clearWarnings() throws SQLException
    {
        connection.clearWarnings();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.sql.Connection#close()
     */
    public void close() throws SQLException
    {
        connection.close();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.sql.Connection#commit()
     */
    public void commit() throws SQLException
    {
        connection.commit();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.sql.Connection#rollback()
     */
    public void rollback() throws SQLException
    {
        connection.rollback();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.sql.Connection#getAutoCommit()
     */
    public boolean getAutoCommit() throws SQLException
    {
        return connection.getAutoCommit();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.sql.Connection#isClosed()
     */
    public boolean isClosed() throws SQLException
    {
        return connection.isClosed();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.sql.Connection#isReadOnly()
     */
    public boolean isReadOnly() throws SQLException
    {
        return connection.isReadOnly();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.sql.Connection#setHoldability(int)
     */
    public void setHoldability(int holdability) throws SQLException
    {
        connection.setHoldability(holdability);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.sql.Connection#setTransactionIsolation(int)
     */
    public void setTransactionIsolation(int level) throws SQLException
    {
        connection.setTransactionIsolation(level);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.sql.Connection#setAutoCommit(boolean)
     */
    public void setAutoCommit(boolean autoCommit) throws SQLException
    {
        connection.setAutoCommit(autoCommit);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.sql.Connection#setReadOnly(boolean)
     */
    public void setReadOnly(boolean readOnly) throws SQLException
    {
        connection.setReadOnly(readOnly);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.sql.Connection#getCatalog()
     */
    public String getCatalog() throws SQLException
    {
        return connection.getCatalog();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.sql.Connection#setCatalog(java.lang.String)
     */
    public void setCatalog(String catalog) throws SQLException
    {
        connection.setCatalog(catalog);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.sql.Connection#getMetaData()
     */
    public DatabaseMetaData getMetaData() throws SQLException
    {
        return connection.getMetaData();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.sql.Connection#getWarnings()
     */
    public SQLWarning getWarnings() throws SQLException
    {
        return connection.getWarnings();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.sql.Connection#setSavepoint()
     */
    public Savepoint setSavepoint() throws SQLException
    {
        return connection.setSavepoint();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.sql.Connection#releaseSavepoint(java.sql.Savepoint)
     */
    public void releaseSavepoint(Savepoint savepoint) throws SQLException
    {
        connection.releaseSavepoint(savepoint);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.sql.Connection#rollback(java.sql.Savepoint)
     */
    public void rollback(Savepoint savepoint) throws SQLException
    {
        connection.rollback();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.sql.Connection#createStatement()
     */
    public Statement createStatement() throws SQLException
    {
        Statement st = connection.createStatement();
        return (Statement) Proxy.newProxyInstance(Statement.class.getClassLoader(),
                                                  new Class[]{Statement.class}, new StatementInvocationHandler(this, st));
    }

    /*
     * (non-Javadoc)
     *
     * @see java.sql.Connection#createStatement(int, int)
     */
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException
    {
        Statement st = connection.createStatement(resultSetType, resultSetConcurrency);
        return (Statement) Proxy.newProxyInstance(Statement.class.getClassLoader(),
                                                  new Class[]{Statement.class}, new StatementInvocationHandler(this, st));
    }

    /*
     * (non-Javadoc)
     *
     * @see java.sql.Connection#createStatement(int, int, int)
     */
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException
    {
        Statement st = connection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        return (Statement) Proxy.newProxyInstance(Statement.class.getClassLoader(),
                                                  new Class[]{Statement.class}, new StatementInvocationHandler(this, st));
    }

    /*
     * (non-Javadoc)
     *
     * @see java.sql.Connection#getTypeMap()
     */
    public Map getTypeMap() throws SQLException
    {
        return connection.getTypeMap();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.sql.Connection#setTypeMap(java.util.Map)
     */
    public void setTypeMap(Map map) throws SQLException
    {
        connection.setTypeMap(map);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.sql.Connection#nativeSQL(java.lang.String)
     */
    public String nativeSQL(String sql) throws SQLException
    {
        return connection.nativeSQL(sql);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.sql.Connection#prepareCall(java.lang.String)
     */
    public CallableStatement prepareCall(String sql) throws SQLException
    {
        CallableStatement cs = connection.prepareCall(sql);
        return (CallableStatement) Proxy.newProxyInstance(CallableStatement.class.getClassLoader(),
                                                          new Class[]{CallableStatement.class}, new StatementInvocationHandler(this, cs));
    }

    /*
     * (non-Javadoc)
     *
     * @see java.sql.Connection#prepareCall(java.lang.String, int, int)
     */
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException
    {
        CallableStatement cs = connection.prepareCall(sql, resultSetType, resultSetConcurrency);
        return (CallableStatement) Proxy.newProxyInstance(CallableStatement.class.getClassLoader(),
                                                          new Class[]{CallableStatement.class}, new StatementInvocationHandler(this, cs));
    }

    /*
     * (non-Javadoc)
     *
     * @see java.sql.Connection#prepareCall(java.lang.String, int, int, int)
     */
    public CallableStatement prepareCall(String sql,
                                         int resultSetType,
                                         int resultSetConcurrency,
                                         int resultSetHoldability) throws SQLException
    {
        CallableStatement cs = connection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        return (CallableStatement) Proxy.newProxyInstance(CallableStatement.class.getClassLoader(),
                                                          new Class[]{CallableStatement.class}, new StatementInvocationHandler(this, cs));
    }

    /*
     * (non-Javadoc)
     *
     * @see java.sql.Connection#prepareStatement(java.lang.String)
     */
    public PreparedStatement prepareStatement(String sql) throws SQLException
    {
        PreparedStatement ps = connection.prepareStatement(sql);
        return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                                                          new Class[]{PreparedStatement.class}, new StatementInvocationHandler(this, ps));
    }

    /*
     * (non-Javadoc)
     *
     * @see java.sql.Connection#prepareStatement(java.lang.String, int)
     */
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException
    {
        PreparedStatement ps = connection.prepareStatement(sql, autoGeneratedKeys);
        return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                                                          new Class[]{PreparedStatement.class}, new StatementInvocationHandler(this, ps));
    }

    /*
     * (non-Javadoc)
     *
     * @see java.sql.Connection#prepareStatement(java.lang.String, int, int)
     */
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException
    {
        PreparedStatement ps = connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
        return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                                                          new Class[]{PreparedStatement.class}, new StatementInvocationHandler(this, ps));
    }

    /*
     * (non-Javadoc)
     *
     * @see java.sql.Connection#prepareStatement(java.lang.String, int, int, int)
     */
    public PreparedStatement prepareStatement(String sql,
                                              int resultSetType,
                                              int resultSetConcurrency,
                                              int resultSetHoldability) throws SQLException
    {
        PreparedStatement ps = connection.prepareStatement(sql, resultSetType, resultSetConcurrency,
                                                           resultSetHoldability);
        return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                                                          new Class[]{PreparedStatement.class}, new StatementInvocationHandler(this, ps));
    }

    /*
     * (non-Javadoc)
     *
     * @see java.sql.Connection#prepareStatement(java.lang.String, int[])
     */
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException
    {
        PreparedStatement ps = connection.prepareStatement(sql, columnIndexes);
        return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                                                          new Class[]{PreparedStatement.class}, new StatementInvocationHandler(this, ps));
    }

    /*
     * (non-Javadoc)
     *
     * @see java.sql.Connection#setSavepoint(java.lang.String)
     */
    public Savepoint setSavepoint(String name) throws SQLException
    {
        return connection.setSavepoint(name);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.sql.Connection#prepareStatement(java.lang.String,
     *      java.lang.String[])
     */
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException
    {
        PreparedStatement ps = connection.prepareStatement(sql, columnNames);
        return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                                                          new Class[]{PreparedStatement.class}, new StatementInvocationHandler(this, ps));
    }

    protected void enlist() throws Exception
    {
        if (isEnlisted())
        {
            return;
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("Enlistment request: " + this);
        }

        UMOTransaction transaction = TransactionCoordination.getInstance().getTransaction();
        if (transaction == null)
        {
            throw new IllegalTransactionStateException(CoreMessages.noMuleTransactionAvailable());
        }
        if (!(transaction instanceof XaTransaction))
        {
            throw new IllegalTransactionStateException(CoreMessages.notMuleXaTransaction(transaction));
        }
        if (!isEnlisted())
        {
            enlisted = ((XaTransaction) transaction).enlistResource(xaConnection.getXAResource());
        }
    }

    public boolean isEnlisted()
    {
        return enlisted;
    }

    public void setEnlisted(boolean enlisted)
    {
        this.enlisted = enlisted;
    }
}
