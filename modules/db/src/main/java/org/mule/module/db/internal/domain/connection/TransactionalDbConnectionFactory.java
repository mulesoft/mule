/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.connection;

import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.module.db.internal.domain.transaction.DbTransactionManager;
import org.mule.module.db.internal.domain.transaction.TransactionalAction;
import org.mule.module.db.internal.domain.type.DbTypeManager;
import org.mule.module.db.internal.resolver.param.GenericParamTypeResolverFactory;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Creates connections using a {@link org.mule.module.db.internal.domain.transaction.DbTransactionManager} to track active transactions.
 */
public class TransactionalDbConnectionFactory implements DbConnectionFactory
{

    protected final Log logger = LogFactory.getLog(getClass());

    protected final DbTransactionManager dbTransactionManager;
    protected final DbTypeManager dbTypeManager;
    private final ConnectionFactory connectionFactory;
    private final DataSource dataSource;

    public TransactionalDbConnectionFactory(DbTransactionManager dbTransactionManager, DbTypeManager dbTypeManager, ConnectionFactory connectionFactory, DataSource dataSource)
    {
        this.dbTransactionManager = dbTransactionManager;
        this.dbTypeManager = dbTypeManager;
        this.connectionFactory = connectionFactory;
        this.dataSource = dataSource;
    }

    @Override
    public DbConnection createConnection(TransactionalAction transactionalAction) throws SQLException
    {
        Connection connection;

        try
        {
            connection = createDataSourceConnection(transactionalAction);
        }
        catch (ConnectionCreationException e)
        {
            throw new SQLException(e);
        }

        return doCreateDbConnection(connection, transactionalAction);
    }

    private Connection createDataSourceConnection(TransactionalAction transactionalAction) throws SQLException
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
                connection = getConnectionFromTransaction(tx, dataSource);
            }
        }
        else if (transactionalAction == TransactionalAction.JOIN_IF_POSSIBLE)
        {
            if (tx == null)
            {
                connection = connectionFactory.create(dataSource);
            }
            else
            {
                connection = getConnectionFromTransaction(tx, dataSource);
            }
        }
        else if (transactionalAction == TransactionalAction.NOT_SUPPORTED)
        {
            connection = connectionFactory.create(dataSource);
        }
        else
        {
            throw new IllegalArgumentException("There is no defined way to manage transactional action " + transactionalAction);
        }
        return connection;
    }

    protected DbConnection doCreateDbConnection(Connection connection, TransactionalAction transactionalAction)
    {
        return new DefaultDbConnection(connection, transactionalAction, new DefaultDbConnectionReleaser(this), new GenericParamTypeResolverFactory(dbTypeManager));
    }

    private Connection getConnectionFromTransaction(Transaction tx, DataSource dataSource) throws SQLException
    {
        Connection con;

        if (tx.hasResource(dataSource))
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Retrieving connection from current transaction: " + tx);
            }
            con = (Connection) tx.getResource(dataSource);
        }
        else
        {
            con = connectionFactory.create(dataSource);

            try
            {
                tx.bindResource(dataSource, con);
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
            RuntimeException exception = null;
            try
            {
                if (!connection.getAutoCommit())
                {
                    connection.commit();
                }
            }
            catch (SQLException e)
            {
                exception = new ConnectionCommitException(e);
            }
            finally
            {
                try
                {
                    connection.close();
                }
                catch (SQLException e)
                {
                    if (exception == null)
                    {
                        exception = new ConnectionClosingException(e);
                    }
                }
            }
            if (exception != null)
            {
                throw exception;
            }
        }
    }

}