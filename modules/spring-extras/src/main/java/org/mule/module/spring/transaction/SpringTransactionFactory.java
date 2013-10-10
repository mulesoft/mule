/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
