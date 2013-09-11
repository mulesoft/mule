/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc;

import org.mule.api.MuleContext;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.api.transaction.UniversalTransactionFactory;

public class JdbcTransactionFactory implements UniversalTransactionFactory
{

    @Override
    public Transaction beginTransaction(MuleContext muleContext) throws TransactionException
    {
        JdbcTransaction tx = new JdbcTransaction(muleContext);
        tx.begin();
        return tx;
    }

    @Override
    public boolean isTransacted()
    {
        return true;
    }

    @Override
    public Transaction createUnboundTransaction(MuleContext muleContext) throws TransactionException
    {
        return new JdbcTransaction(muleContext);
    }
}
