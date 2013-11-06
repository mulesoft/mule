/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.btm.transaction;

import org.mule.util.xa.DefaultXASession;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

/**
 * JTA transaction wrapper that keep track of the fake XAResource instances created for mule core queues.
 */
public class TransactionWrapper implements Transaction
{

    private final Transaction delegate;
    private final TransactionManagerWrapper transactionalManagerWrapper;

    public TransactionWrapper(Transaction transaction, TransactionManagerWrapper transactionManagerWrapper)
    {
        this.delegate = transaction;
        this.transactionalManagerWrapper = transactionManagerWrapper;
    }

    @Override
    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException
    {
        delegate.commit();
    }

    @Override
    public boolean delistResource(XAResource xaResource, int i) throws IllegalStateException, SystemException
    {
        return delegate.delistResource(xaResource, i);
    }

    @Override
    public boolean enlistResource(XAResource xaResource) throws RollbackException, IllegalStateException, SystemException
    {
        if (xaResource instanceof DefaultXASession)
        {
            transactionalManagerWrapper.setCurrentXaResource((DefaultXASession) xaResource);
        }
        return delegate.enlistResource(xaResource);
    }

    @Override
    public int getStatus() throws SystemException
    {
        return delegate.getStatus();
    }

    @Override
    public void registerSynchronization(Synchronization synchronization) throws RollbackException, IllegalStateException, SystemException
    {
        delegate.registerSynchronization(synchronization);
    }

    @Override
    public void rollback() throws IllegalStateException, SystemException
    {
        delegate.rollback();
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException
    {
        delegate.setRollbackOnly();
    }
}
