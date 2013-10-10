/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
