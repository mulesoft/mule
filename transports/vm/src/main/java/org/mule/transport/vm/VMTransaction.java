/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.vm;

import org.mule.api.MuleContext;
import org.mule.api.transaction.TransactionException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transaction.AbstractSingleResourceTransaction;
import org.mule.transaction.IllegalTransactionStateException;
import org.mule.util.queue.QueueManager;
import org.mule.util.queue.QueueSession;
import org.mule.util.xa.ResourceManagerException;

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

    protected void doBegin() throws TransactionException
    {
    }

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
