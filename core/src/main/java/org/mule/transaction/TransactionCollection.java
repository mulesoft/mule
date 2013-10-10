/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transaction;

import org.mule.api.transaction.Transaction;

import java.util.List;

/**
 * Marks a collection of transactions, e.g. a set of single-resource TXs managed as
 * a single unit of work, but without resorting to XA.
 */
public interface TransactionCollection
{
    void aggregate(AbstractSingleResourceTransaction tx);

    List<Transaction> getTxCollection();
}
