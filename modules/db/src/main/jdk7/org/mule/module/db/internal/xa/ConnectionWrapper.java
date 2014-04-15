/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.xa;


import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transaction.IllegalTransactionStateException;
import org.mule.transaction.TransactionCoordination;
import org.mule.transaction.XaTransaction;

import java.lang.reflect.Proxy;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Using for unification XAConnection and Connection
 */
public class ConnectionWrapper implements Connection, XaTransaction.MuleXaObject
{

    private final XAConnection xaConnection;
    private Connection connection;

    /**
     * This is the lock object that guards access to {@link #enlistedXAResource}.
     */
    private final Object enlistedXAResourceLock = new Object();
    /**
     * @GuardedBy {@link #enlistedXAResourceLock}
     */
    private XAResource enlistedXAResource;

    protected static final transient Log logger = LogFactory.getLog(ConnectionWrapper.class);
    private volatile boolean reuseObject = false;

    public ConnectionWrapper(XAConnection xaCon) throws SQLException
    {
        this.xaConnection = xaCon;
        this.connection = xaCon.getConnection();
    }

    @Override
    public int getHoldability() throws SQLException
    {
        return connection.getHoldability();
    }

    @Override
    public int getTransactionIsolation() throws SQLException
    {
        return connection.getTransactionIsolation();
    }

    @Override
    public void clearWarnings() throws SQLException
    {
        connection.clearWarnings();
    }

    @Override
    public void close() throws SQLException
    {
        connection.close();
        try
        {
            xaConnection.close();
        }
        catch (SQLException e)
        {
            logger.info(
                    "Exception while explicitely closing the xaConnection (some providers require this). "
                    + "The exception will be ignored and only logged: " + e.getMessage(), e);
        }
    }

    @Override
    public void commit() throws SQLException
    {
        connection.commit();
    }

    @Override
    public void rollback() throws SQLException
    {
        connection.rollback();
    }

    @Override
    public boolean getAutoCommit() throws SQLException
    {
        return connection.getAutoCommit();
    }

    @Override
    public boolean isClosed() throws SQLException
    {
        return connection.isClosed();
    }

    @Override
    public boolean isReadOnly() throws SQLException
    {
        return connection.isReadOnly();
    }

    @Override
    public void setHoldability(int holdability) throws SQLException
    {
        connection.setHoldability(holdability);
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException
    {
        connection.setTransactionIsolation(level);
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException
    {
        connection.setAutoCommit(autoCommit);
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException
    {
        connection.setReadOnly(readOnly);
    }

    @Override
    public String getCatalog() throws SQLException
    {
        return connection.getCatalog();
    }

    @Override
    public void setCatalog(String catalog) throws SQLException
    {
        connection.setCatalog(catalog);
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException
    {
        return connection.getMetaData();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException
    {
        return connection.getWarnings();
    }

    @Override
    public Savepoint setSavepoint() throws SQLException
    {
        return connection.setSavepoint();
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException
    {
        connection.releaseSavepoint(savepoint);
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException
    {
        connection.rollback();
    }

    @Override
    public Statement createStatement() throws SQLException
    {
        Statement st = connection.createStatement();
        return (Statement) Proxy.newProxyInstance(Statement.class.getClassLoader(),
                                                  new Class[] {Statement.class}, new StatementInvocationHandler(st));
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException
    {
        Statement st = connection.createStatement(resultSetType, resultSetConcurrency);
        return (Statement) Proxy.newProxyInstance(Statement.class.getClassLoader(),
                                                  new Class[] {Statement.class}, new StatementInvocationHandler(st));
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException
    {
        Statement st = connection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        return (Statement) Proxy.newProxyInstance(Statement.class.getClassLoader(),
                                                  new Class[] {Statement.class}, new StatementInvocationHandler(st));
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException
    {
        return connection.getTypeMap();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException
    {
        connection.setTypeMap(map);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException
    {
        return connection.nativeSQL(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException
    {
        CallableStatement cs = connection.prepareCall(sql);
        return (CallableStatement) Proxy.newProxyInstance(CallableStatement.class.getClassLoader(),
                                                          new Class[] {CallableStatement.class}, new StatementInvocationHandler(cs));
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException
    {
        CallableStatement cs = connection.prepareCall(sql, resultSetType, resultSetConcurrency);
        return (CallableStatement) Proxy.newProxyInstance(CallableStatement.class.getClassLoader(),
                                                          new Class[] {CallableStatement.class}, new StatementInvocationHandler(cs));
    }

    @Override
    public CallableStatement prepareCall(String sql,
                                         int resultSetType,
                                         int resultSetConcurrency,
                                         int resultSetHoldability) throws SQLException
    {
        CallableStatement cs = connection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        return (CallableStatement) Proxy.newProxyInstance(CallableStatement.class.getClassLoader(),
                                                          new Class[] {CallableStatement.class}, new StatementInvocationHandler(cs));
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException
    {
        PreparedStatement ps = connection.prepareStatement(sql);
        return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                                                          new Class[] {PreparedStatement.class}, new StatementInvocationHandler(ps));
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException
    {
        PreparedStatement ps = connection.prepareStatement(sql, autoGeneratedKeys);
        return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                                                          new Class[] {PreparedStatement.class}, new StatementInvocationHandler(ps));
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException
    {
        PreparedStatement ps = connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
        return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                                                          new Class[] {PreparedStatement.class}, new StatementInvocationHandler(ps));
    }

    @Override
    public PreparedStatement prepareStatement(String sql,
                                              int resultSetType,
                                              int resultSetConcurrency,
                                              int resultSetHoldability) throws SQLException
    {
        PreparedStatement ps = connection.prepareStatement(sql, resultSetType, resultSetConcurrency,
                                                           resultSetHoldability);
        return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                                                          new Class[] {PreparedStatement.class}, new StatementInvocationHandler(ps));
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException
    {
        PreparedStatement ps = connection.prepareStatement(sql, columnIndexes);
        return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                                                          new Class[] {PreparedStatement.class}, new StatementInvocationHandler(ps));
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException
    {
        return connection.setSavepoint(name);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException
    {
        PreparedStatement ps = connection.prepareStatement(sql, columnNames);
        return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                                                          new Class[] {PreparedStatement.class}, new StatementInvocationHandler(ps));
    }

    @Override
    public boolean enlist() throws TransactionException
    {
        try
        {
            connection.setAutoCommit(false);
        }
        catch (SQLException e)
        {
            throw new TransactionException(e);
        }

        if (isEnlisted())
        {
            return false;
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("Enlistment request: " + this);
        }

        Transaction transaction = TransactionCoordination.getInstance().getTransaction();
        if (transaction == null)
        {
            throw new IllegalTransactionStateException(CoreMessages.noMuleTransactionAvailable());
        }
        if (!(transaction instanceof XaTransaction))
        {
            throw new IllegalTransactionStateException(CoreMessages.notMuleXaTransaction(transaction));
        }

        synchronized (enlistedXAResourceLock)
        {
            if (!isEnlisted())
            {
                final XAResource xaResource = getXAResourceFromXATransaction();
                boolean wasAbleToEnlist = ((XaTransaction) transaction).enlistResource(xaResource);
                if (wasAbleToEnlist)
                {
                    enlistedXAResource = xaResource;
                }
            }
        }

        return isEnlisted();
    }

    protected XAResource getXAResourceFromXATransaction() throws TransactionException
    {
        try
        {
            return xaConnection.getXAResource();
        }
        catch (SQLException e)
        {
            throw new TransactionException(e);
        }
    }

    @Override
    public boolean delist() throws Exception
    {
        if (!isEnlisted())
        {
            return false;
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("Delistment request: " + this);
        }

        Transaction transaction = TransactionCoordination.getInstance().getTransaction();
        if (transaction == null)
        {
            throw new IllegalTransactionStateException(CoreMessages.noMuleTransactionAvailable());
        }
        if (!(transaction instanceof XaTransaction))
        {
            throw new IllegalTransactionStateException(CoreMessages.notMuleXaTransaction(transaction));
        }

        synchronized (enlistedXAResourceLock)
        {
            if (isEnlisted())
            {
                boolean wasAbleToDelist = ((XaTransaction) transaction).delistResource(enlistedXAResource,
                                                                                       XAResource.TMSUCCESS);
                if (wasAbleToDelist)
                {
                    enlistedXAResource = null;
                }
            }
            return !isEnlisted();
        }
    }

    public boolean isEnlisted()
    {
        synchronized (enlistedXAResourceLock)
        {
            return enlistedXAResource != null;
        }
    }

    @Override
    public boolean isReuseObject()
    {
        return reuseObject;
    }

    @Override
    public void setReuseObject(boolean reuseObject)
    {
        this.reuseObject = reuseObject;
    }

    @Override
    public Object getTargetObject()
    {
        return xaConnection;
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException
    {
        return connection.createArrayOf(typeName, elements);
    }

    @Override
    public Blob createBlob() throws SQLException
    {
        return connection.createBlob();
    }

    @Override
    public Clob createClob() throws SQLException
    {
        return connection.createClob();
    }

    @Override
    public NClob createNClob() throws SQLException
    {
        return connection.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException
    {
        return connection.createSQLXML();
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException
    {
        return connection.createStruct(typeName, attributes);
    }

    @Override
    public Properties getClientInfo() throws SQLException
    {
        return connection.getClientInfo();
    }

    @Override
    public String getClientInfo(String name) throws SQLException
    {
        return connection.getClientInfo(name);
    }

    @Override
    public boolean isValid(int timeout) throws SQLException
    {
        return connection.isValid(timeout);
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException
    {
        connection.setClientInfo(properties);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException
    {
        connection.setClientInfo(name, value);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException
    {
        return connection.isWrapperFor(iface);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException
    {
        return connection.unwrap(iface);
    }

    @Override
    public void setSchema(String schema) throws SQLException
    {
        connection.setSchema(schema);
    }

    @Override
    public String getSchema() throws SQLException
    {
        return connection.getSchema();
    }

    @Override
    public void abort(Executor executor) throws SQLException
    {
        connection.abort(executor);
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException
    {
        connection.setNetworkTimeout(executor, milliseconds);
    }

    @Override
    public int getNetworkTimeout() throws SQLException
    {
        return connection.getNetworkTimeout();
    }

}
