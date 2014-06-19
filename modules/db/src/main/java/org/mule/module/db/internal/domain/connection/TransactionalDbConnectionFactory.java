/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.connection;

import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.module.db.internal.domain.database.DbConfig;
import org.mule.module.db.internal.domain.transaction.DbTransactionManager;
import org.mule.module.db.internal.domain.transaction.TransactionalAction;
import org.mule.module.db.internal.domain.type.DbTypeManager;
import org.mule.module.db.internal.resolver.param.GenericParamTypeResolverFactory;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Creates connections using a {@link org.mule.module.db.internal.domain.transaction.DbTransactionManager} to track active transactions.
 */
public class TransactionalDbConnectionFactory implements DbConnectionFactory
{

    protected final Log logger = LogFactory.getLog(getClass());

    protected final DbConfig dbConfig;
    protected final DbTransactionManager dbTransactionManager;
    protected final DbTypeManager dbTypeManager;

    public TransactionalDbConnectionFactory(DbConfig dbConfig, DbTransactionManager dbTransactionManager, DbTypeManager dbTypeManager)
    {
        this.dbConfig = dbConfig;
        this.dbTransactionManager = dbTransactionManager;
        this.dbTypeManager = dbTypeManager;
    }

    @Override
    public DbConnection createConnection(TransactionalAction transactionalAction) throws SQLException
    {
        Transaction tx = dbTransactionManager.getTransaction();

        Connection connection;

        if (transactionalAction == TransactionalAction.ALWAYS_JOIN)
        {
            if (tx == null)
            {
                throw new IllegalStateException("Transactional action is " + transactionalAction + " but there is no active transaction");
            }
            else
            {
                connection = getConnectionFromTransaction(dbConfig, tx);
            }
        }
        else if (transactionalAction == TransactionalAction.JOIN_IF_POSSIBLE)
        {
            if (tx == null)
            {
                connection = getConnectionFromDataSource(dbConfig);
            }
            else
            {
                connection = getConnectionFromTransaction(dbConfig, tx);
            }
        }
        else if (transactionalAction == TransactionalAction.NOT_SUPPORTED)
        {
            connection = getConnectionFromDataSource(dbConfig);
        }
        else
        {
            throw new IllegalArgumentException("There is no defined way to manage transactional action " + transactionalAction);
        }

        return doCreateDbConnection(connection, transactionalAction);
    }

    protected DbConnection doCreateDbConnection(Connection connection, TransactionalAction transactionalAction)
    {
        return new DefaultDbConnection(connection, transactionalAction, new DefaultDbConnectionReleaser(this), new GenericParamTypeResolverFactory(dbTypeManager));
    }

    private Connection getConnectionFromDataSource(DbConfig config)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Retrieving new connection from data source");
        }

        try
        {
            return config.getDataSource().getConnection();
        }
        catch (Exception e)
        {
            throw new ConnectionCreationException(e);
        }
    }

    private Connection getConnectionFromTransaction(DbConfig config, Transaction tx) throws SQLException
    {
        Connection con;

        if (tx.hasResource(config.getDataSource()))
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Retrieving connection from current transaction: " + tx);
            }
            con = (Connection) tx.getResource(config.getDataSource());
        }
        else
        {
            con = getConnectionFromDataSource(config);

            try
            {
                tx.bindResource(config.getDataSource(), con);
            }
            catch (TransactionException e)
            {
                if (con != null && !con.isClosed())
                {
                    con.close();
                }

                throw new ConnectionBindingException("Could not bind connection to current transaction: " + tx, e);
            }
        }

        return con;
    }

    @Override
    public void releaseConnection(DbConnection connection)
    {
        if (connection == null)
        {
            return;
        }

        try
        {
            if (connection.isClosed())
            {
                return;
            }
        }
        catch (SQLException e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Error checking for closed connection on releasing connection", e);
            }
            return;
        }
        final Transaction transaction = dbTransactionManager.getTransaction();
        boolean closeConnection = false;
        if (connection.getTransactionalAction() == TransactionalAction.NOT_SUPPORTED)
        {
            closeConnection = true;
        }
        else if (connection.getTransactionalAction() == TransactionalAction.JOIN_IF_POSSIBLE && transaction == null)
        {
            closeConnection = true;
        }

        if (closeConnection)
        {
            try
            {
                if (!connection.getAutoCommit())
                {
                    connection.commit();
                }
            }
            catch (SQLException e)
            {
                throw new ConnectionCommitException(e);
            }

            try
            {
                connection.close();
            }
            catch (SQLException e)
            {
                throw new ConnectionClosingException(e);
            }
        }
    }
}