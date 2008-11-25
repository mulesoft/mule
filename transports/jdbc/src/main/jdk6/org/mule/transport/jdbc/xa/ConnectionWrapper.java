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
    private volatile boolean enlisted = false;
    protected static final transient Log logger = LogFactory.getLog(ConnectionWrapper.class);
    private volatile boolean reuseObject = false;

    public ConnectionWrapper(XAConnection xaCon) throws SQLException
    {
        this.xaConnection = xaCon;
        this.connection = xaCon.getConnection();
    }

    public int getHoldability() throws SQLException
    {
        return connection.getHoldability();
    }

    public int getTransactionIsolation() throws SQLException
    {
        return connection.getTransactionIsolation();
    }

    public void clearWarnings() throws SQLException
    {
        connection.clearWarnings();
    }

    public void close() throws SQLException
    {
        connection.close();
    }

    public void commit() throws SQLException
    {
        connection.commit();
    }

    public void rollback() throws SQLException
    {
        connection.rollback();
    }

    public boolean getAutoCommit() throws SQLException
    {
        return connection.getAutoCommit();
    }

    public boolean isClosed() throws SQLException
    {
        return connection.isClosed();
    }

    public boolean isReadOnly() throws SQLException
    {
        return connection.isReadOnly();
    }

    public void setHoldability(int holdability) throws SQLException
    {
        connection.setHoldability(holdability);
    }

    public void setTransactionIsolation(int level) throws SQLException
    {
        connection.setTransactionIsolation(level);
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException
    {
        connection.setAutoCommit(autoCommit);
    }

    public void setReadOnly(boolean readOnly) throws SQLException
    {
        connection.setReadOnly(readOnly);
    }

    public String getCatalog() throws SQLException
    {
        return connection.getCatalog();
    }

    public void setCatalog(String catalog) throws SQLException
    {
        connection.setCatalog(catalog);
    }

    public DatabaseMetaData getMetaData() throws SQLException
    {
        return connection.getMetaData();
    }

    public SQLWarning getWarnings() throws SQLException
    {
        return connection.getWarnings();
    }

    public Savepoint setSavepoint() throws SQLException
    {
        return connection.setSavepoint();
    }

    public void releaseSavepoint(Savepoint savepoint) throws SQLException
    {
        connection.releaseSavepoint(savepoint);
    }

    public void rollback(Savepoint savepoint) throws SQLException
    {
        connection.rollback();
    }

    public Statement createStatement() throws SQLException
    {
        Statement st = connection.createStatement();
        return (Statement) Proxy.newProxyInstance(Statement.class.getClassLoader(),
                                                  new Class[]{Statement.class}, new StatementInvocationHandler(this, st));
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException
    {
        Statement st = connection.createStatement(resultSetType, resultSetConcurrency);
        return (Statement) Proxy.newProxyInstance(Statement.class.getClassLoader(),
                                                  new Class[]{Statement.class}, new StatementInvocationHandler(this, st));
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException
    {
        Statement st = connection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        return (Statement) Proxy.newProxyInstance(Statement.class.getClassLoader(),
                                                  new Class[]{Statement.class}, new StatementInvocationHandler(this, st));
    }

    public Map getTypeMap() throws SQLException
    {
        return connection.getTypeMap();
    }

    public void setTypeMap(Map<String, Class<?>> map) throws SQLException
    {
        connection.setTypeMap(map);
    }

    public String nativeSQL(String sql) throws SQLException
    {
        return connection.nativeSQL(sql);
    }

    public CallableStatement prepareCall(String sql) throws SQLException
    {
        CallableStatement cs = connection.prepareCall(sql);
        return (CallableStatement) Proxy.newProxyInstance(CallableStatement.class.getClassLoader(),
                                                          new Class[]{CallableStatement.class}, new StatementInvocationHandler(this, cs));
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException
    {
        CallableStatement cs = connection.prepareCall(sql, resultSetType, resultSetConcurrency);
        return (CallableStatement) Proxy.newProxyInstance(CallableStatement.class.getClassLoader(),
                                                          new Class[]{CallableStatement.class}, new StatementInvocationHandler(this, cs));
    }

    public CallableStatement prepareCall(String sql,
                                         int resultSetType,
                                         int resultSetConcurrency,
                                         int resultSetHoldability) throws SQLException
    {
        CallableStatement cs = connection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        return (CallableStatement) Proxy.newProxyInstance(CallableStatement.class.getClassLoader(),
                                                          new Class[]{CallableStatement.class}, new StatementInvocationHandler(this, cs));
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException
    {
        PreparedStatement ps = connection.prepareStatement(sql);
        return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                                                          new Class[]{PreparedStatement.class}, new StatementInvocationHandler(this, ps));
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException
    {
        PreparedStatement ps = connection.prepareStatement(sql, autoGeneratedKeys);
        return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                                                          new Class[]{PreparedStatement.class}, new StatementInvocationHandler(this, ps));
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException
    {
        PreparedStatement ps = connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
        return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                                                          new Class[]{PreparedStatement.class}, new StatementInvocationHandler(this, ps));
    }

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

    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException
    {
        PreparedStatement ps = connection.prepareStatement(sql, columnIndexes);
        return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                                                          new Class[]{PreparedStatement.class}, new StatementInvocationHandler(this, ps));
    }

    public Savepoint setSavepoint(String name) throws SQLException
    {
        return connection.setSavepoint(name);
    }

    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException
    {
        PreparedStatement ps = connection.prepareStatement(sql, columnNames);
        return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                                                          new Class[]{PreparedStatement.class}, new StatementInvocationHandler(this, ps));
    }

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
        if (!isEnlisted())
        {
            final XAResource xaResource;
            try
            {
                xaResource = xaConnection.getXAResource();
            }
            catch (SQLException e)
            {
                throw new TransactionException(e);
            }
            
            enlisted = ((XaTransaction) transaction).enlistResource(xaResource);
        }
        
        return enlisted;
    }

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
        if (isEnlisted())
        {
            enlisted = !((XaTransaction) transaction).delistResource(xaConnection.getXAResource(), XAResource.TMSUCCESS);
        }
        return !isEnlisted();
    }


    public boolean isEnlisted()
    {
        return enlisted;
    }

    public void setEnlisted(boolean enlisted)
    {
        this.enlisted = enlisted;
    }

    public boolean isReuseObject()
    {
        return reuseObject;
    }

    public void setReuseObject(boolean reuseObject)
    {
        this.reuseObject = reuseObject;
    }

    public Object getTargetObject()
    {
        return xaConnection;
    }

    public Array createArrayOf(String typeName, Object[] elements) throws SQLException
    {
        return connection.createArrayOf(typeName, elements);
    }

    public Blob createBlob() throws SQLException
    {
        return connection.createBlob();
    }

    public Clob createClob() throws SQLException
    {
        return connection.createClob();
    }

    public NClob createNClob() throws SQLException
    {
        return connection.createNClob();
    }

    public SQLXML createSQLXML() throws SQLException
    {
        return connection.createSQLXML();
    }

    public Struct createStruct(String typeName, Object[] attributes) throws SQLException
    {
        return connection.createStruct(typeName, attributes);
    }

    public Properties getClientInfo() throws SQLException
    {
        return connection.getClientInfo();
    }

    public String getClientInfo(String name) throws SQLException
    {
        return connection.getClientInfo(name);
    }

    public boolean isValid(int timeout) throws SQLException
    {
        return connection.isValid(timeout);
    }

    public void setClientInfo(Properties properties) throws SQLClientInfoException
    {
        connection.setClientInfo(properties);
    }

    public void setClientInfo(String name, String value) throws SQLClientInfoException
    {
        connection.setClientInfo(name, value);
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException
    {
        return connection.isWrapperFor(iface);
    }

    public <T> T unwrap(Class<T> iface) throws SQLException
    {
        return connection.unwrap(iface);
    }
}
