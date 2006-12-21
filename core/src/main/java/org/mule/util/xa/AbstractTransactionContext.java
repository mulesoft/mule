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

import javax.transaction.Status;

import org.safehaus.uuid.UUIDGenerator;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class AbstractTransactionContext
{

    private static UUIDGenerator gen = UUIDGenerator.getInstance();

    protected String id = gen.generateTimeBasedUUID().toString();
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

}
