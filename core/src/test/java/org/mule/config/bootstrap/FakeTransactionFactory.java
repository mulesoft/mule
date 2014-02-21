package org.mule.config.bootstrap;

import org.mule.api.MuleContext;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.api.transaction.UniversalTransactionFactory;

public final class FakeTransactionFactory implements UniversalTransactionFactory
{

    @Override
    public Transaction beginTransaction(MuleContext muleContext) throws TransactionException 
    {
        return null;
    }

    @Override
    public boolean isTransacted() 
    {
        return false;
    }

    @Override
    public Transaction createUnboundTransaction(MuleContext muleContext) throws TransactionException 
    {
        return null;
    }

}