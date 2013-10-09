/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.context.notification;

import org.mule.api.context.notification.BlockingServerEvent;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.transaction.Transaction;

public class TransactionNotification extends ServerNotification implements BlockingServerEvent
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

    public TransactionNotification(Transaction transaction, int action)
    {
        super(transaction.getId(), action, transaction.getId());
        this.transactionStringId = transaction.getId();
    }

    public String getTransactionStringId()
    {
        return this.transactionStringId;
    }

    @Override
    public String toString()
    {
        return EVENT_NAME + "{" + "action=" + getActionName(action) + ", transactionStringId=" + transactionStringId
               + ", timestamp=" + timestamp + "}";
    }

}
