/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
}
