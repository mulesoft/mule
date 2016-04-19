/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.vm;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.transaction.TransactionException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.transaction.AbstractSingleResourceTransaction;
import org.mule.runtime.core.transaction.IllegalTransactionStateException;
import org.mule.runtime.core.util.queue.QueueManager;
import org.mule.runtime.core.util.queue.QueueSession;
import org.mule.runtime.core.util.xa.ResourceManagerException;

public class VMTransaction extends AbstractSingleResourceTransaction
{

    public VMTransaction(MuleContext muleContext) throws TransactionException
    {
        this(muleContext, false);
    }

    public VMTransaction(MuleContext muleContext, boolean initialize) throws TransactionException
    {
        super(muleContext);
        if (initialize)
        {
            QueueManager qm = muleContext.getQueueManager();
            QueueSession qs = qm.getQueueSession();
            bindResource(qm, qs);
        }
    }

    @Override
    public void bindResource(Object key, Object resource) throws TransactionException
    {
        if (!(key instanceof QueueManager) || !(resource instanceof QueueSession))
        {
            throw new IllegalTransactionStateException(
                CoreMessages.transactionCanOnlyBindToResources("QueueManager/QueueSession"));
        }
        super.bindResource(key, resource);
        try
        {
            ((QueueSession)resource).begin();
        }
        catch (ResourceManagerException e)
        {
            throw new TransactionException(CoreMessages.cannotStartTransaction("VMTransaction"), e);
        }
    }

    @Override
    protected void doBegin() throws TransactionException
    {
    }

    @Override
    protected void doCommit() throws TransactionException
    {
        try
        {
            if (resource != null)
            {
                ((QueueSession)resource).commit();
            }
        }
        catch (ResourceManagerException e)
        {
            throw new TransactionException(CoreMessages.transactionCommitFailed(), e);
        }
    }

    @Override
    protected void doRollback() throws TransactionException
    {
        try
        {
            if (resource != null)
            {
                ((QueueSession)resource).rollback();
            }
        }
        catch (ResourceManagerException e)
        {
            throw new TransactionException(CoreMessages.transactionRollbackFailed(), e);
        }
    }

    @Override
    protected Class getResourceType()
    {
        return QueueSession.class;
    }

    @Override
    protected Class getKeyType()
    {
        return QueueManager.class;
    }
}
