/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.xa;

import java.util.HashMap;
import java.util.Map;

import javax.transaction.xa.Xid;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public abstract class AbstractXAResourceManager extends AbstractResourceManager
{

    protected Map suspendedContexts = new HashMap();
    protected Map activeContexts = new HashMap();

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
