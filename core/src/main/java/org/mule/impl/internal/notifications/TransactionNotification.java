/*
* $Id$
* --------------------------------------------------------------------------------------
* Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
*
* The software in this package is published under the terms of the CPAL v1.0
* license, a copy of which has been included with this distribution in the
* LICENSE.txt file.
*/

package org.mule.impl.internal.notifications;

import org.mule.umo.UMOTransaction;
import org.mule.umo.manager.UMOServerNotification;

public class TransactionNotification extends UMOServerNotification implements BlockingServerEvent
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -3245036187011582121L;
    public static final int TRANSACTION_BEGAN = TRANSACTION_EVENT_ACTION_START_RANGE + 1;
    public static final int TRANSACTION_COMMITTED = TRANSACTION_EVENT_ACTION_START_RANGE + 2;
    public static final int TRANSACTION_ROLLEDBACK = TRANSACTION_EVENT_ACTION_START_RANGE + 3;

    static
    {
        registerAction("begin", TRANSACTION_BEGAN);
        registerAction("commit", TRANSACTION_COMMITTED);
        registerAction("rollback", TRANSACTION_ROLLEDBACK);
    }

    /**
     * Ideally, that should've been a transaction's XID, but we'd need to resort to all kinds of reflection tricks to
     * get it. Still, toString() typically outputs a class name followed by the XID, so that's good enough.
     */
    private String transactionStringId;

    public TransactionNotification(UMOTransaction transaction, int action)
    {
        super(transaction, action);
        this.transactionStringId = transaction.toString();
    }

    public String getTransactionStringId()
    {
        return this.transactionStringId;
    }

    public String toString()
    {
        return EVENT_NAME + "{" + "action=" + getActionName(action) + ", transactionStringId=" + transactionStringId
               + ", timestamp=" + timestamp + "}";
    }

}
