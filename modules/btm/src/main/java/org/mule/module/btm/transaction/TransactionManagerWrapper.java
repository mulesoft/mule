/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.btm.transaction;

import org.mule.module.btm.xa.DefaultXaSessionResourceProducer;
import org.mule.util.xa.DefaultXASession;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

/**
 * Wrapper for BTM transaction manager that adds logic around mule core queue functionality and transaction recovery.
 */
public class TransactionManagerWrapper implements TransactionManager
{

    private final TransactionManager delegate;
    private final DefaultXaSessionResourceProducer defaultXaSessionResourceProducer;
    private ThreadLocal<DefaultXASession> defaultXASessionThreadLocal = new ThreadLocal<DefaultXASession>();

    public TransactionManagerWrapper(TransactionManager transactionManager, DefaultXaSessionResourceProducer defaultXaSessionResourceProducer)
    {
        this.delegate = transactionManager;
        this.defaultXaSessionResourceProducer = defaultXaSessionResourceProducer;
    }

    @Override
    public void begin() throws NotSupportedException, SystemException
    {
        delegate.begin();
    }

    @Override
    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException
    {
        delegate.commit();
        if (defaultXASessionThreadLocal.get() != null)
        {
            defaultXaSessionResourceProducer.removeDefaultXASession(defaultXASessionThreadLocal.get());
            defaultXASessionThreadLocal.remove();
        }
    }

    @Override
    public int getStatus() throws SystemException
    {
        return delegate.getStatus();
    }

    @Override
    public Transaction getTransaction() throws SystemException
    {
        return new TransactionWrapper(delegate.getTransaction(), this);
    }

    @Override
    public void resume(Transaction transaction) throws InvalidTransactionException, IllegalStateException, SystemException
    {
        delegate.resume(transaction);
    }

    @Override
    public void rollback() throws IllegalStateException, SecurityException, SystemException
    {
        try
        {
            delegate.rollback();
        }
        finally
        {
            if (defaultXASessionThreadLocal.get() != null)
            {
                defaultXaSessionResourceProducer.removeDefaultXASession(defaultXASessionThreadLocal.get());
                defaultXASessionThreadLocal.remove();
            }
        }
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException
    {
        delegate.setRollbackOnly();
    }

    @Override
    public void setTransactionTimeout(int i) throws SystemException
    {
        delegate.setTransactionTimeout(i);
    }

    @Override
    public Transaction suspend() throws SystemException
    {
        return delegate.suspend();
    }

    public void setCurrentXaResource(DefaultXASession defaultXASession)
    {
        defaultXASessionThreadLocal.set(defaultXASession);
        defaultXaSessionResourceProducer.addDefaultXASession(defaultXASession);
    }
}
