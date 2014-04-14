/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.transaction;

import org.mule.api.MuleContext;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.api.transaction.TransactionFactory;

/**
 * Creates database transactions
 */
public class DbTransactionFactory implements TransactionFactory
{

    @Override
    public Transaction beginTransaction(MuleContext muleContext) throws TransactionException
    {
        DbTransaction tx = new DbTransaction(muleContext);
        tx.begin();
        return tx;
    }

    @Override
    public boolean isTransacted()
    {
        return true;
    }
}