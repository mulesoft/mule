/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.vm;

import org.mule.config.i18n.CoreMessages;
import org.mule.transaction.AbstractSingleResourceTransaction;
import org.mule.transaction.IllegalTransactionStateException;
import org.mule.umo.TransactionException;
import org.mule.umo.UMOManagementContext;
import org.mule.util.queue.QueueManager;
import org.mule.util.queue.QueueSession;
import org.mule.util.xa.ResourceManagerException;

public class VMTransaction extends AbstractSingleResourceTransaction
{

    public VMTransaction(UMOManagementContext managementContext) throws TransactionException
    {
        QueueManager qm = managementContext.getQueueManager();
        QueueSession qs = qm.getQueueSession();
        bindResource(qm, qs);
    }

    public void bindResource(Object key, Object resource) throws TransactionException
    {
        if (!(key instanceof QueueManager) || !(resource instanceof QueueSession))
        {
            throw new IllegalTransactionStateException(
                CoreMessages.transactionCanOnlyBindToResources("QueueManager/QueueSession"));
        }
        super.bindResource(key, resource);
    }

    protected void doBegin() throws TransactionException
    {
        try
        {
            ((QueueSession)resource).begin();
        }
        catch (ResourceManagerException e)
        {
            throw new TransactionException(CoreMessages.cannotStartTransaction("VMTransaction"), e);
        }
    }

    protected void doCommit() throws TransactionException
    {
        try
        {
            ((QueueSession)resource).commit();
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
            ((QueueSession)resource).rollback();
        }
        catch (ResourceManagerException e)
        {
            throw new TransactionException(CoreMessages.transactionRollbackFailed(), e);
        }
    }

}
