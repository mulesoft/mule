/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.queue;

import org.mule.util.xa.AbstractXAResourceManager;
import org.mule.util.xa.DefaultXASession;
import org.mule.util.xa.XaTransactionRecoverer;

import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

public class QueueXaResource extends DefaultXASession<XaQueueTypeTransactionContextAdapter>
{

    private final XaTransactionRecoverer xaTransactionRecoverer;
    private final QueueProvider queueProvider;

    public QueueXaResource(AbstractXAResourceManager xaResourceManager, XaTransactionRecoverer xaTransactionRecoverer, QueueProvider queueProvider)
    {
        super(xaResourceManager);
        this.xaTransactionRecoverer = xaTransactionRecoverer;
        this.queueProvider = queueProvider;
    }

    // XA transaction implementation
    @Override
    protected void commitDanglingTransaction(Xid xid, boolean onePhase) throws XAException
    {
        xaTransactionRecoverer.commitDandlingTransaction(xid, onePhase);
    }

    @Override
    protected void rollbackDandlingTransaction(Xid xid) throws XAException
    {
        xaTransactionRecoverer.rollbackDandlingTransaction(xid);
    }

    @Override
    protected XaQueueTypeTransactionContextAdapter createTransactionContext(Xid xid)
    {
        return new XaQueueTypeTransactionContextAdapter(xaTransactionRecoverer.getXaTxQueueTransactionJournal(), queueProvider, xid);
    }

    @Override
    public Xid[] recover(int i) throws XAException
    {
        return xaTransactionRecoverer.recover(i);
    }
}
