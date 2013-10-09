/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.transaction;

import org.mule.api.MuleContext;


/**
 * <code>ExternalTransactionAwareTransactionFactory</code> creates a transaction, possibly representing a transaction
 * started outside Mule.
 *
 */
public interface ExternalTransactionAwareTransactionFactory extends TransactionFactory
{
    /**
     * Create and begins a new transaction
     *
     * @return a new Transaction representing an existing external transaction
     * @throws org.mule.api.transaction.TransactionException if the transaction cannot be created or begun
     * @param muleContext
     */
    Transaction joinExternalTransaction(MuleContext muleContext) throws TransactionException;
}
