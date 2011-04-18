/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc.store;

import java.io.Serializable;
import java.sql.SQLException;
import java.text.MessageFormat;

import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.ArrayHandler;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.store.ObjectDoesNotExistException;
import org.mule.api.store.ObjectStoreException;
import org.mule.api.transaction.TransactionCallback;
import org.mule.api.transaction.TransactionConfig;
import org.mule.config.i18n.CoreMessages;
import org.mule.transaction.TransactionTemplate;
import org.mule.transport.jdbc.JdbcConnector;
import org.mule.util.store.AbstractMonitoredObjectStore;

public class JdbcObjectStore extends AbstractMonitoredObjectStore<String>
{

    public static final String selectQueryTemplate = "SELECT * FROM {0} WHERE {1} = ?";
    public static final String deleteQueryTemplate = "DELETE FROM {0} WHERE {1} = ?";
    public static final String insertQueryTemplate = "INSERT INTO {0}({1}, {2}) VALUES(?, ?)";

    private JdbcConnector jdbcConnector;
    private TransactionConfig transactionConfig;
    private String tableName;
    private String keyColumn;
    private String valueColumn;

    private String insertQuery;
    private String deleteQuery;
    private String selectQuery;

    private ArrayHandler arrayHandler;

    public JdbcObjectStore()
    {
        this.arrayHandler = new ArrayHandler();
        this.maxEntries = -1;
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
    public void initialise() throws InitialisationException
    {
        if (this.insertQuery == null)
        {
            // initialize if it is not overridden
            this.insertQuery = MessageFormat.format(insertQueryTemplate, this.getTableName(),
                this.getKeyColumn(), this.getValueColumn());
        }
        this.deleteQuery = MessageFormat.format(deleteQueryTemplate, this.getTableName(), this.getKeyColumn());
        this.selectQuery = MessageFormat.format(selectQueryTemplate, this.getTableName(), this.getKeyColumn());
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(Serializable key) throws ObjectStoreException
    {
        this.notNullKey(key);
        Object[] result = (Object[]) this.query(selectQuery, this.arrayHandler, key);
        return result != null;
    }

    /**
     * {@inheritDoc}
     */
    public String remove(Serializable key) throws ObjectStoreException
    {
        this.notNullKey(key);
        String value = this.retrieve(key);
        this.update(deleteQuery, key);
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public String retrieve(Serializable key) throws ObjectStoreException
    {
        Object[] row = (Object[]) this.query(selectQuery, this.arrayHandler, key);
        if (row == null)
        {
            throw new ObjectDoesNotExistException(CoreMessages.objectNotFound(key));
        }
        else
        {
            return (String) row[1];
        }
    }

    public void store(Serializable key, String value, String[] parameters) throws ObjectStoreException
    {
        Object[] arguments = new Object[2 + parameters.length];
        arguments[0] = key;
        arguments[1] = value;

        for (int i = 0; i < parameters.length; i++)
        {
            String parameter = parameters[i];
            arguments[2 + i] = parameter;
        }

        this.update(insertQuery, arguments);
    }

    /**
     * {@inheritDoc}
     */
    public void store(Serializable key, String value) throws ObjectStoreException
    {
        this.update(insertQuery, key, value);
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

    public String getTableName()
    {
        if (this.tableName == null)
        {
            this.tableName = "ids";
        }
        return tableName;
    }

    public void setTableName(String tableName)
    {
        this.tableName = tableName;
    }

    public String getKeyColumn()
    {
        if (this.keyColumn == null)
        {
            this.keyColumn = "key_column";
        }
        return keyColumn;
    }

    public void setKeyColumn(String keyColumn)
    {
        this.keyColumn = keyColumn;
    }

    public String getValueColumn()
    {
        if (this.valueColumn == null)
        {
            this.valueColumn = "value_column";
        }
        return valueColumn;
    }

    public void setValueColumn(String valueColumn)
    {
        this.valueColumn = valueColumn;
    }

    public String getInsertQuery()
    {
        return insertQuery;
    }

    public void setInsertQuery(String insertQuery)
    {
        this.insertQuery = insertQuery;
    }
}
