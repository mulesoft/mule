/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.xa;

import org.mule.util.UUID;

import javax.transaction.Status;

public abstract class AbstractTransactionContext
{
    protected String id = UUID.getUUID();
    protected long timeout;
    protected int status;
    protected boolean readOnly;
    protected boolean suspended;
    protected boolean finished;

    public AbstractTransactionContext()
    {
        status = Status.STATUS_NO_TRANSACTION;
        suspended = false;
        finished = false;
        readOnly = true;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(id).append("[");
        sb.append(getStatusString());
        if (suspended)
        {
            sb.append(", suspended");
        }
        if (readOnly)
        {
            sb.append(", readonly");
        }
        if (finished)
        {
            sb.append(", finished");
        }
        sb.append("]");
        return sb.toString();
    }

    private String getStatusString()
    {
        switch (status)
        {
            case Status.STATUS_ACTIVE :
                return "active";
            case Status.STATUS_MARKED_ROLLBACK :
                return "marked rollback";
            case Status.STATUS_PREPARED :
                return "prepared";
            case Status.STATUS_COMMITTED :
                return "committed";
            case Status.STATUS_ROLLEDBACK :
                return "rolled back";
            case Status.STATUS_UNKNOWN :
                return "unknown";
            case Status.STATUS_NO_TRANSACTION :
                return "no transaction";
            case Status.STATUS_PREPARING :
                return "preparing";
            case Status.STATUS_COMMITTING :
                return "committing";
            case Status.STATUS_ROLLING_BACK :
                return "rolling back";
            default :
                return "undefined status";
        }
    }

    public synchronized void finalCleanUp() throws ResourceManagerException
    {
        // nothing to do (yet?)
    }

    public synchronized void notifyFinish()
    {
        finished = true;
        notifyAll();
    }

    public abstract void doCommit() throws ResourceManagerException;

    public abstract void doRollback() throws ResourceManagerException;
}
