/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
