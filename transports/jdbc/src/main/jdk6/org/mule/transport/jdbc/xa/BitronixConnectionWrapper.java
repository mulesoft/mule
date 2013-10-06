/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
import java.util.concurrent.Executor;

import javax.transaction.xa.XAResource;

import bitronix.tm.resource.jdbc.JdbcConnectionHandle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Bitronix connection wrapper for making XAConnection participate in Mule XA transactions.
 */
public class BitronixConnectionWrapper implements Connection, XaTransaction.MuleXaObject
{

    protected static final transient Log logger = LogFactory.getLog(BitronixConnectionWrapper.class);
    private Connection connection;

    public BitronixConnectionWrapper(Connection connection) throws SQLException
    {
        this.connection = connection;
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

    @Override
    public void setReuseObject(boolean reuseObject)
    {
        //Do not support reuse object since that is defined by the btm pool
    }

    @Override
    public boolean isReuseObject()
    {
        //Do not support reuse object since that is defined by the btm pool
        return false;
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

        Transaction transaction = TransactionCoordination.getInstance().getTransaction();
        if (transaction == null)
        {
            throw new IllegalTransactionStateException(CoreMessages.noMuleTransactionAvailable());
        }
        if (!(transaction.isXA()))
        {
            throw new IllegalTransactionStateException(CoreMessages.notMuleXaTransaction(transaction));
        }

        return ((XaTransaction) transaction).enlistResource(getXaResource());
    }

    private XAResource getXaResource()
    {
        return ((JdbcConnectionHandle) ((Proxy) connection).getInvocationHandler(connection)).getPooledConnection().getXAResource();
    }

    @Override
    public boolean delist() throws Exception
    {
        Transaction transaction = TransactionCoordination.getInstance().getTransaction();
        if (transaction == null)
        {
            throw new IllegalTransactionStateException(CoreMessages.noMuleTransactionAvailable());
        }
        if (!(transaction instanceof XaTransaction))
        {
            throw new IllegalTransactionStateException(CoreMessages.notMuleXaTransaction(transaction));
        }

        return ((XaTransaction) transaction).delistResource(getXaResource(), XAResource.TMSUCCESS);
    }

    @Override
    public Object getTargetObject()
    {
        return connection;
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
        return connection.createStatement();
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException
    {
        return connection.createStatement(resultSetType, resultSetConcurrency);
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException
    {
        return connection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
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
        return connection.prepareCall(sql);
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException
    {
        return connection.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    public CallableStatement prepareCall(String sql,
                                         int resultSetType,
                                         int resultSetConcurrency,
                                         int resultSetHoldability) throws SQLException
    {
        return connection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException
    {
        return connection.prepareStatement(sql);
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException
    {
        return connection.prepareStatement(sql, autoGeneratedKeys);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException
    {
        return connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    public PreparedStatement prepareStatement(String sql,
                                              int resultSetType,
                                              int resultSetConcurrency,
                                              int resultSetHoldability) throws SQLException
    {
        return connection.prepareStatement(sql, resultSetType, resultSetConcurrency,
                                           resultSetHoldability);
    }

    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException
    {
        return connection.prepareStatement(sql, columnIndexes);
    }

    public Savepoint setSavepoint(String name) throws SQLException
    {
        return connection.setSavepoint(name);
    }

    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException
    {
        return connection.prepareStatement(sql, columnNames);
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
