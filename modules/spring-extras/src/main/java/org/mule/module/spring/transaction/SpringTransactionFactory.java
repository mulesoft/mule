/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.spring.transaction;

import org.mule.api.MuleContext;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.api.transaction.TransactionFactory;
import org.mule.transaction.AbstractSingleResourceTransaction;

import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jms.connection.JmsResourceHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * TODO: document this class
 */
public class SpringTransactionFactory implements TransactionFactory
{

    private PlatformTransactionManager manager;

    public SpringTransactionFactory()
    {
        super();
    }

    public Transaction beginTransaction(MuleContext muleContext) throws TransactionException
    {
        Transaction tx = new SpringTransaction(muleContext);
        tx.begin();
        return tx;
    }

    public boolean isTransacted()
    {
        return true;
    }

    /**
     * @return Returns the manager.
     */
    synchronized public PlatformTransactionManager getManager()
    {
        return manager;
    }

    /**
     * @param manager The manager to set.
     */
    synchronized public void setManager(PlatformTransactionManager manager)
    {
        this.manager = manager;
    }

    /**
     * TODO: document this class
     */
    public class SpringTransaction extends AbstractSingleResourceTransaction
    {
        protected final TransactionStatus status;

        public SpringTransaction(MuleContext muleContext)
        {
            super(muleContext);
            status = manager.getTransaction(null);
        }

        protected void doBegin() throws TransactionException
        {
            // nothing to do
        }

        protected void doCommit() throws TransactionException
        {
           manager.commit(status);
        }

        protected void doRollback() throws TransactionException
        {
           manager.rollback(status);
        }

        public Object getResource(Object key)
        {
            Object res = TransactionSynchronizationManager.getResource(key);
            if (res != null)
            {
                if (!(res instanceof ConnectionHolder))
                {
                    if (res instanceof JmsResourceHolder)
                    {
                        return ((JmsResourceHolder)res).getConnection();
                    }
                }
                else
                {
                    return ((ConnectionHolder)res).getConnection();
                }
            }
            return res;
        }

        public boolean hasResource(Object key)
        {
            return getResource(key) != null;
        }

        public void bindResource(Object key, Object resource) throws TransactionException
        {
            throw new UnsupportedOperationException();
        }

        public void setRollbackOnly()
        {
            super.setRollbackOnly();
            status.setRollbackOnly();
        }

    }

}
