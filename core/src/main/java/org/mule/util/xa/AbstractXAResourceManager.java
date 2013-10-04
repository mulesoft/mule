/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.xa;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.xa.Xid;

public abstract class AbstractXAResourceManager extends AbstractResourceManager
{

    protected Map suspendedContexts = new ConcurrentHashMap();
    protected Map activeContexts = new ConcurrentHashMap();

    public AbstractXAResourceManager()
    {
        super();
    }

    protected boolean includeBranchInXid()
    {
        return true;
    }

    AbstractTransactionContext getTransactionalResource(Xid xid)
    {
        AbstractTransactionContext context = getActiveTransactionalResource(xid);
        if (context != null)
        {
            return context;
        }
        else
        {
            return getSuspendedTransactionalResource(xid);
        }
    }

    AbstractTransactionContext getActiveTransactionalResource(Xid xid)
    {
        return (AbstractTransactionContext) activeContexts.get(xid);
    }

    AbstractTransactionContext getSuspendedTransactionalResource(Xid xid)
    {
        return (AbstractTransactionContext) suspendedContexts.get(xid);
    }

    void addActiveTransactionalResource(Xid xid, AbstractTransactionContext context)
    {
        activeContexts.put(xid, context);
    }

    void addSuspendedTransactionalResource(Xid xid, AbstractTransactionContext context)
    {
        suspendedContexts.put(xid, context);
    }

    void removeActiveTransactionalResource(Xid xid)
    {
        activeContexts.remove(xid);
    }

    void removeSuspendedTransactionalResource(Xid xid)
    {
        suspendedContexts.remove(xid);
    }

}
