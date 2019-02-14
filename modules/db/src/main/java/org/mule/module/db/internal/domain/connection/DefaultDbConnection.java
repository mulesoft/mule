/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.connection;

import org.mule.module.db.internal.domain.transaction.TransactionalAction;
import org.mule.module.db.internal.domain.type.DbType;
import org.mule.module.db.internal.resolver.param.ParamTypeResolverFactory;
import org.mule.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import static java.lang.String.format;
import static org.mule.module.db.internal.domain.type.JdbcTypes.BLOB_DB_TYPE;
import static org.mule.module.db.internal.domain.type.JdbcTypes.CLOB_DB_TYPE;
import static org.mule.util.IOUtils.toByteArray;

/**
 * Delegates {@link Connection} behaviour to a delegate
 */
public class DefaultDbConnection extends AbstractDbConnection
{
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final int UNKNOWN_DATA_TYPE = -1;

    public static final int DATA_TYPE_INDEX = 5;

    public static final int ATTR_TYPE_NAME_INDEX = 6;

    public DefaultDbConnection(Connection delegate, TransactionalAction transactionalAction, DefaultDbConnectionReleaser connectionReleaseListener, ParamTypeResolverFactory paramTypeResolverFactory)
    {
        super(delegate, transactionalAction, connectionReleaseListener, paramTypeResolverFactory);
    }

    @Override
    public Statement createStatement() throws SQLException
    {
        return delegate.createStatement();
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException
    {
        return delegate.prepareStatement(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException
    {
        return delegate.prepareCall(sql);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException
    {
        return delegate.nativeSQL(sql);
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException
    {
        delegate.setAutoCommit(autoCommit);
    }

    @Override
    public boolean getAutoCommit() throws SQLException
    {
        return delegate.getAutoCommit();
    }

    @Override
    public void commit() throws SQLException
    {
        delegate.commit();
    }

    @Override
    public void rollback() throws SQLException
    {
        delegate.rollback();
    }

    @Override
    public void close() throws SQLException
    {
        delegate.close();
    }

    @Override
    public boolean isClosed() throws SQLException
    {
        return delegate.isClosed();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException
    {
        return delegate.getMetaData();
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException
    {
        delegate.setReadOnly(readOnly);
    }

    @Override
    public boolean isReadOnly() throws SQLException
    {
        return delegate.isReadOnly();
    }

    @Override
    public void setCatalog(String catalog) throws SQLException
    {
        delegate.setCatalog(catalog);
    }

    @Override
    public String getCatalog() throws SQLException
    {
        return delegate.getCatalog();
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException
    {
        delegate.setTransactionIsolation(level);
    }

    @Override
    public int getTransactionIsolation() throws SQLException
    {
        return delegate.getTransactionIsolation();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException
    {
        return delegate.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException
    {
        delegate.clearWarnings();
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException
    {
        return delegate.createStatement(resultSetType, resultSetConcurrency);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
    {
        return delegate.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
    {
        return delegate.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException
    {
        return delegate.getTypeMap();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> stringClassMap) throws SQLException
    {
        delegate.setTypeMap(stringClassMap);
    }

    @Override
    public void setHoldability(int holdability) throws SQLException
    {
        delegate.setHoldability(holdability);
    }

    @Override
    public int getHoldability() throws SQLException
    {
        return delegate.getHoldability();
    }

    @Override
    public Savepoint setSavepoint() throws SQLException
    {
        return delegate.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException
    {
        return delegate.setSavepoint(name);
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException
    {
        delegate.rollback(savepoint);
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException
    {
        delegate.releaseSavepoint(savepoint);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        return delegate.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        return delegate.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        return delegate.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException
    {
        return delegate.prepareStatement(sql, autoGeneratedKeys);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException
    {
        return delegate.prepareStatement(sql, columnIndexes);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException
    {
        return delegate.prepareStatement(sql, columnNames);
    }

    @Override
    public Clob createClob() throws SQLException
    {
        return delegate.createClob();
    }

    @Override
    public Blob createBlob() throws SQLException
    {
        return delegate.createBlob();
    }

    @Override
    public NClob createNClob() throws SQLException
    {
        return delegate.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException
    {
        return delegate.createSQLXML();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException
    {
        return delegate.isValid(timeout);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException
    {
        delegate.setClientInfo(name, value);
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException
    {
        delegate.setClientInfo(properties);
    }

    @Override
    public String getClientInfo(String name) throws SQLException
    {
        return delegate.getClientInfo(name);
    }

    @Override
    public Properties getClientInfo() throws SQLException
    {
        return delegate.getClientInfo();
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException
    {
        try
        {
            resolveLobsForArrays(typeName, elements);
        }
        catch (SQLException e)
        {
            logger.warn("Unable to resolve lobs: {}. Proceeding with original attributes.", e.getMessage());
        }
        return delegate.createArrayOf(typeName, elements);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException
    {
        try
        {
            resolveLobs(typeName, attributes);
        }
        catch (SQLException e)
        {
            logger.warn("Unable to resolve lobs: {}. Proceeding with original attributes.", e.getMessage());
        }
        return delegate.createStruct(typeName, attributes);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException
    {
        return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException
    {
        return delegate.isWrapperFor(iface);
    }

    @Override
    public void setSchema(String schema) throws SQLException
    {
        delegate.setSchema(schema);
    }

    @Override
    public String getSchema() throws SQLException
    {
        return delegate.getSchema();
    }

    @Override
    public void abort(Executor executor) throws SQLException
    {
        delegate.abort(executor);
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException
    {
        delegate.setNetworkTimeout(executor, milliseconds);
    }

    @Override
    public int getNetworkTimeout() throws SQLException
    {
        return delegate.getNetworkTimeout();
    }

    @Override
    protected void resolveLobs(String typeName, Object[] attributes) throws SQLException
    {
        Map<Integer, List> dataTypes = getDataTypes(typeName);

        for(Integer key : dataTypes.keySet())
        {
            List dataType = dataTypes.get(key);
            doResolveLobIn(attributes, key, (int) dataType.get(0), (String) dataType.get(1));
        }
    }

    private Map<Integer, List> getDataTypes(String typeName) throws SQLException
    {
        Map<Integer, List> dataTypes = new HashMap<>();

        try (ResultSet resultSet = this.getMetaData().getAttributes(this.getCatalog(), null, typeName, null))
        {
            List<String> lobTypes = Arrays.asList(BLOB_DB_TYPE.getName(), CLOB_DB_TYPE.getName());
            int index = 0;
            while (resultSet.next())
            {
                int dataType = resultSet.getInt(DATA_TYPE_INDEX);
                String dataTypeName = resultSet.getString(ATTR_TYPE_NAME_INDEX);

                if(lobTypes.contains(dataTypeName))
                {
                    dataTypes.put(index, Arrays.asList(dataType, dataTypeName));
                }

                index++;
            }
        }
        return dataTypes;
    }

    protected void resolveLobsForArrays(String typeName, Object[] attributes) throws SQLException
    {
        Map<Integer, List> dataTypes = getDataTypes(typeName);

        for(Integer key : dataTypes.keySet())
        {
            List dataType = dataTypes.get(key);
            for(Object attribute : attributes)
            {
                doResolveLobIn((Object[]) attribute, key, (int) dataType.get(0), (String) dataType.get(1));
            }
        }
    }

    protected void doResolveLobIn(Object[] attributes, int index, int dataType, String dataTypeName) throws SQLException
    {
        if (shouldResolveAttributeWithJdbcType(dataType, dataTypeName, BLOB_DB_TYPE))
        {
            attributes[index] = createBlob(attributes[index]);
        }
        else if (shouldResolveAttributeWithJdbcType(dataType, dataTypeName, CLOB_DB_TYPE))
        {
            attributes[index] = createClob(attributes[index]);
        }
    }

    private boolean shouldResolveAttributeWithJdbcType(int dbDataType, String dbDataTypeName, DbType jdbcType)
    {
        if (dbDataType == UNKNOWN_DATA_TYPE)
        {
            return dbDataTypeName.equals(jdbcType.getName());
        }
        else
        {
            return dbDataType == jdbcType.getId();
        }
    }

    protected void doResolveLobIn(Object[] attributes, int index, String dataTypeName) throws SQLException
    {
        doResolveLobIn(attributes, index, UNKNOWN_DATA_TYPE, dataTypeName);
    }


    protected Blob createBlob(Object attribute) throws SQLException
    {
        Blob blob = this.createBlob();
        if (attribute instanceof byte[])
        {
            blob.setBytes(1, (byte[]) attribute);
        }
        else if (attribute instanceof InputStream)
        {
            blob.setBytes(1, toByteArray((InputStream) attribute));
        }
        else if (attribute instanceof String)
        {
            blob.setBytes(1, ((String) attribute).getBytes());
        }
        else
        {
            throw new IllegalArgumentException(format("Cannot create a %s from a value of type '%s'", Struct.class.getName(), attribute.getClass()));
        }

        return blob;
    }

    protected Clob createClob(Object attribute) throws SQLException
    {
        Clob clob = this.createClob();
        if (attribute instanceof String)
        {
            clob.setString(1, (String) attribute);
        }
        else if (attribute instanceof InputStream)
        {
            clob.setString(1, IOUtils.toString((InputStream) attribute));
        }
        else
        {
            throw new IllegalArgumentException(format("Cannot create a %s from a value of type '%s'", Struct.class.getName(), attribute.getClass()));
        }

        return clob;
    }
}
