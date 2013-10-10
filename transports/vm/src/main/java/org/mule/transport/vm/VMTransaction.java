/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
