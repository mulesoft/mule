package org.mule.transaction;

import org.mule.api.MuleContext;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.api.transaction.TransactionFactory;/*
 * $Id
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */


/** A superclass for transaction factories that cannot join external transactions */
public abstract class AbstractTransactionFactory implements TransactionFactory
{
    public Transaction joinExternalTransaction(MuleContext muleContext) throws TransactionException
    {
        return null;
    }
}
