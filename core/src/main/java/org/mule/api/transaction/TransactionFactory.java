/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.transaction;

import org.mule.api.MuleContext;


/**
 * <code>TransactionFactory</code> creates a transaction.
 */
public interface TransactionFactory
{
    /**
     * Create and begins a new transaction
     * 
     * @return a new Transaction
     * @throws TransactionException if the transaction cannot be created or begun
     * @param muleContext
     */
    Transaction beginTransaction(MuleContext muleContext) throws TransactionException;

    /**
     * Determines whether this transaction factory creates transactions that are
     * really transacted or if they are being used to simulate batch actions, such as
     * using Jms Client Acknowledge.
     */
    boolean isTransacted();
}
