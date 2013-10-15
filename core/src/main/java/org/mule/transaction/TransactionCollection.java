/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
