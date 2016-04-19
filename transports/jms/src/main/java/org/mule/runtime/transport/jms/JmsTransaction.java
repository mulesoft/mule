/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.transaction.TransactionException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.transaction.AbstractSingleResourceTransaction;
import org.mule.runtime.core.transaction.IllegalTransactionStateException;
import org.mule.runtime.transport.jms.i18n.JmsMessages;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;

/**
 * <code>JmsTransaction</code> is a wrapper for a JMS local transaction. This
 * object holds the JMS session and controls when the transaction is committed or
 * rolled back.
 */
public class JmsTransaction extends AbstractSingleResourceTransaction
{

    public JmsTransaction(MuleContext muleContext)
    {
        super(muleContext);
    }

    @Override
    public void bindResource(Object key, Object resource) throws TransactionException
    {
        if (!(key instanceof Connection) || !(resource instanceof Session))
        {
            throw new IllegalTransactionStateException(
                CoreMessages.transactionCanOnlyBindToResources("javax.jms.Connection/javax.jms.Session"));
        }

        Session session = (Session)resource;
        try
        {
            if (!session.getTransacted())
            {
                throw new IllegalTransactionStateException(JmsMessages.sessionShouldBeTransacted());
            }
        }
        catch (JMSException e)
        {
            throw new IllegalTransactionStateException(CoreMessages.transactionCannotReadState(), e);
        }

        super.bindResource(key, resource);
    }



    @Override
    protected void doBegin() throws TransactionException
    {
        // do nothing
    }

    @Override
    protected void doCommit() throws TransactionException
    {
        if (resource == null)
        {
            logger.warn(CoreMessages.commitTxButNoResource(this));
            return;
        }

        try
        {
            ((Session) resource).commit();
        }
        catch (JMSException e)
        {
            throw new TransactionException(CoreMessages.transactionCommitFailed(), e);
        }
        finally
        {
            try
            {
                ((Session) resource).close();
            }
            catch (JMSException e)
            {
                logger.warn("could not close jms session", e);
            }
        }
    }

    @Override
    protected void doRollback() throws TransactionException
    {
        if (resource == null)
        {
            logger.warn(CoreMessages.rollbackTxButNoResource(this));
            return;
        }

        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Rolling back transaction: " + getId());
            }
            ((Session) resource).rollback();
        }
        catch (JMSException e)
        {
            throw new TransactionException(CoreMessages.transactionRollbackFailed(), e);
        }
        finally
        {
            try
            {
                ((Session) resource).close();
            }
            catch (JMSException e)
            {
                logger.warn("could not close jms session", e);
            }
        }
    }

    @Override
    protected Class getResourceType()
    {
        return Session.class;
    }

    @Override
    protected Class getKeyType()
    {
        return Connection.class;
    }
}
