/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jdbc;

import org.mule.api.MuleContext;
import org.mule.api.transaction.TransactionException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transaction.AbstractSingleResourceTransaction;
import org.mule.transaction.IllegalTransactionStateException;
import org.mule.transaction.TransactionRollbackException;
import org.mule.transport.jdbc.i18n.JdbcMessages;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

/**
 * TODO
 */
public class JdbcTransaction extends AbstractSingleResourceTransaction
{

    public JdbcTransaction(MuleContext muleContext)
    {
        super(muleContext);
    }

    public void bindResource(Object key, Object resource) throws TransactionException
    {
        if (!(key instanceof DataSource) || !(resource instanceof Connection))
        {
            throw new IllegalTransactionStateException(
                CoreMessages.transactionCanOnlyBindToResources("javax.sql.DataSource/java.sql.Connection"));
        }
        Connection con = (Connection)resource;
        try
        {
            if (con.getAutoCommit())
            {
                con.setAutoCommit(false);
            }
        }
        catch (SQLException e)
        {
            throw new TransactionException(JdbcMessages.transactionSetAutoCommitFailed(), e);
        }
        super.bindResource(key, resource);
    }

    protected void doBegin() throws TransactionException
    {
        // Do nothing
    }

    protected void doCommit() throws TransactionException
    {
        if (resource == null)
        {
            logger.warn(CoreMessages.commitTxButNoResource(this));
            return;
        }
        
        try
        {
            ((Connection)resource).commit();
            ((Connection)resource).close();
        }
        catch (SQLException e)
        {
            throw new TransactionException(CoreMessages.transactionCommitFailed(), e);
        }
    }

    protected void doRollback() throws TransactionException
    {
        if (resource == null)
        {
            logger.warn(CoreMessages.rollbackTxButNoResource(this));
            return;
        }

        try
        {
            ((Connection)resource).rollback();
            ((Connection)resource).close();
        }
        catch (SQLException e)
        {
            throw new TransactionRollbackException(CoreMessages.transactionRollbackFailed(), e);
        }
    }

    protected Class<Connection> getResourceType()
    {
        return Connection.class;
    }

    protected Class<DataSource> getKeyType()
    {
        return DataSource.class;
    }
}
