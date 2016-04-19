/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionException;
import org.mule.runtime.core.api.transaction.TransactionFactory;

/**
 * <code>JmsClientAcknowledgeTransactionFactory</code> creates a JMS Client
 * Acknowledge Transaction using a JMS Message.
 */

public class JmsClientAcknowledgeTransactionFactory implements TransactionFactory
{
    @Override
    public Transaction beginTransaction(MuleContext muleContext) throws TransactionException
    {
        JmsClientAcknowledgeTransaction tx = new JmsClientAcknowledgeTransaction(muleContext);
        tx.begin();
        return tx;
    }

    @Override
    public boolean isTransacted()
    {
        return false;
    }
}
