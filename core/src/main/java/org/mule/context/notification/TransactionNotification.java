/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import org.mule.api.MuleContext;
import org.mule.api.context.notification.BlockingServerEvent;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.transaction.Transaction;
import org.mule.util.store.DeserializationPostInitialisable;

public class TransactionNotification extends ServerNotification implements BlockingServerEvent, DeserializationPostInitialisable
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

    private transient MuleContext muleContext;

    /**
     * Ideally, that should've been a transaction's XID, but we'd need to resort to all kinds of reflection tricks to
     * get it. Still, toString() typically outputs a class name followed by the XID, so that's good enough.
     */
    private String transactionStringId;

    public TransactionNotification(Transaction transaction, int action, MuleContext muleContext)
    {
        super(transaction.getId(), action, transaction.getId());
        this.transactionStringId = transaction.getId();
        this.muleContext = muleContext;
    }

    public String getApplicationName()
    {
        return muleContext.getConfiguration().getId();
    }

    public String getTransactionStringId()
    {
        return this.transactionStringId;
    }

    public void initAfterDeserialisation(MuleContext context)
    {
        this.muleContext = context;
    }

    @Override
    public String toString()
    {
        return EVENT_NAME + "{" + "action=" + getActionName(action) + ", transactionStringId=" + transactionStringId
               + ", timestamp=" + timestamp + "}";
    }
}
