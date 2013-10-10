/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc.store;

import java.io.Serializable;
import java.sql.SQLException;

import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.ArrayHandler;
import org.mule.api.store.ObjectAlreadyExistsException;
import org.mule.api.store.ObjectDoesNotExistException;
import org.mule.api.store.ObjectStoreException;
import org.mule.api.transaction.TransactionCallback;
import org.mule.api.transaction.TransactionConfig;
import org.mule.config.i18n.CoreMessages;
import org.mule.transaction.TransactionTemplate;
import org.mule.transport.jdbc.JdbcConnector;
import org.mule.util.store.AbstractMonitoredObjectStore;

public class JdbcObjectStore<T extends Serializable> extends AbstractMonitoredObjectStore<T>
{

    private JdbcConnector jdbcConnector;
    private TransactionConfig transactionConfig;
    private String insertQueryKey;
    private String selectQueryKey;
    private String deleteQueryKey;

    private ArrayHandler arrayHandler;

    public JdbcObjectStore()
    {
        this.arrayHandler = new ArrayHandler();
        this.maxEntries = -1;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isPersistent()
    {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    protected void expire()
    {
        // DO NOTHING
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(Serializable key) throws ObjectStoreException
    {
        this.notNullKey(key);
        Object[] result = (Object[]) this.query(this.getSelectQuery(), this.arrayHandler, key);
        return result != null;
    }

    /**
     * {@inheritDoc}
     */
    public T remove(Serializable key) throws ObjectStoreException
    {
        this.notNullKey(key);
        T value = this.retrieve(key);
        this.update(this.getDeleteQuery(), key);
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public T retrieve(Serializable key) throws ObjectStoreException
    {
        Object[] row = (Object[]) this.query(this.getSelectQuery(), this.arrayHandler, key);
        if (row == null)
        {
            throw new ObjectDoesNotExistException(CoreMessages.objectNotFound(key));
        }
        else
        {
            return (T) row[1];
        }
    }

    public void store(Serializable key, T value, String[] parameters) throws ObjectStoreException
    {
        Object[] arguments = new Object[2 + parameters.length];
        arguments[0] = key;
        arguments[1] = value;

        for (int i = 0; i < parameters.length; i++)
        {
            String parameter = parameters[i];
            arguments[2 + i] = parameter;
        }

        this.update(this.getInsertQuery(), arguments);
    }

    /**
     * {@inheritDoc}
     */
    public void store(Serializable key, T value) throws ObjectStoreException
    {
        this.notNullKey(key);
        try
        {
            this.update(this.getInsertQuery(), key, value);
        }
        catch (ObjectStoreException e)
        {
            throw new ObjectAlreadyExistsException(e);
        }
    }

    /**
     * Validates that the key is not null
     * 
     * @param key
     * @throws ObjectStoreException
     */
    private void notNullKey(Serializable key) throws ObjectStoreException
    {
        if (key == null)
        {
            throw new ObjectStoreException(CoreMessages.objectIsNull("id"));
        }
    }

    /**
     * Executes the query in sql using the current transaction config
     * 
     * @param sql
     * @param handler
     * @param arguments
     * @return
     * @throws ObjectStoreException
     */
    private Object query(final String sql, final ResultSetHandler handler, final Object... arguments)
        throws ObjectStoreException
    {
        try
        {
            TransactionTemplate<Object> tt = new TransactionTemplate<Object>(this.transactionConfig,
                this.jdbcConnector.getMuleContext());

            Object result = tt.execute(new TransactionCallback<Object>()
            {
                public Object doInTransaction() throws Exception
                {
                    return jdbcConnector.getQueryRunner().query(sql, handler, arguments);
                }
            });

            return result;
        }
        catch (SQLException e)
        {
            throw new ObjectStoreException(e);
        }
        catch (Exception e)
        {
            throw new ObjectStoreException(e);
        }
    }

    /**
     * Executes an update query using the current transaction config
     * 
     * @param sql
     * @param arguments
     * @return
     * @throws ObjectStoreException
     */
    private Object update(final String sql, final Object... arguments) throws ObjectStoreException
    {
        try
        {
            TransactionTemplate<Object> tt = new TransactionTemplate<Object>(this.transactionConfig,
                this.jdbcConnector.getMuleContext());

            Object result = tt.execute(new TransactionCallback<Object>()
            {
                public Object doInTransaction() throws Exception
                {
                    return jdbcConnector.getQueryRunner().update(sql, arguments);
                }
            });

            return result;
        }
        catch (SQLException e)
        {
            throw new ObjectStoreException(e);
        }
        catch (Exception e)
        {
            throw new ObjectStoreException(e);
        }
    }

    public JdbcConnector getJdbcConnector()
    {
        return jdbcConnector;
    }

    public void setJdbcConnector(JdbcConnector jdbcConnector)
    {
        this.jdbcConnector = jdbcConnector;
    }

    public TransactionConfig getTransactionConfig()
    {
        return transactionConfig;
    }

    public void setTransactionConfig(TransactionConfig transactionConfig)
    {
        this.transactionConfig = transactionConfig;
    }

    public String getInsertQuery()
    {
        return (String) this.jdbcConnector.getQueries().get(this.insertQueryKey);
    }

    public String getSelectQuery()
    {
        return (String) this.jdbcConnector.getQueries().get(this.selectQueryKey);
    }

    public String getDeleteQuery()
    {
        return (String) this.jdbcConnector.getQueries().get(this.deleteQueryKey);
    }

    public String getInsertQueryKey()
    {
        return insertQueryKey;
    }

    public void setInsertQueryKey(String insertQueryKey)
    {
        this.insertQueryKey = insertQueryKey;
    }

    public String getSelectQueryKey()
    {
        return selectQueryKey;
    }

    public void setSelectQueryKey(String selectQueryKey)
    {
        this.selectQueryKey = selectQueryKey;
    }

    public String getDeleteQueryKey()
    {
        return deleteQueryKey;
    }

    public void setDeleteQueryKey(String deleteQueryKey)
    {
        this.deleteQueryKey = deleteQueryKey;
    }
}
