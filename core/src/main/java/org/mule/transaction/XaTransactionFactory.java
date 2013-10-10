/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transaction;

import org.mule.api.MuleContext;
import org.mule.api.transaction.ExternalTransactionAwareTransactionFactory;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.config.i18n.CoreMessages;

import javax.transaction.TransactionManager;

/**
 * <code>XaTransactionFactory</code> Is used to create/retrieve a Transaction from
 * a transaction manager configured on the MuleManager.
 */
public class XaTransactionFactory implements ExternalTransactionAwareTransactionFactory
{
    public Transaction beginTransaction(MuleContext muleContext) throws TransactionException
    {
        try
        {
            XaTransaction xat = new XaTransaction(muleContext);
            xat.begin();
            return xat;
        }
        catch (Exception e)
        {
            throw new TransactionException(CoreMessages.cannotStartTransaction("XA"), e);
        }
    }

    /**
     * Create a Mule transaction that represents a transaction started outside of Mule
     */
    public Transaction joinExternalTransaction(MuleContext muleContext) throws TransactionException
    {
        try
        {
            TransactionManager txManager = muleContext.getTransactionManager();
            if (txManager.getTransaction() == null)
            {
                return null;
            }
            XaTransaction xat = new ExternalXaTransaction(muleContext);
            xat.begin();
            return xat;
        }
        catch (Exception e)
        {
            throw new TransactionException(CoreMessages.cannotStartTransaction("XA"), e);
        }
    }

    /**
     * Determines whether this transaction factory creates transactions that are
     * really transacted or if they are being used to simulate batch actions, such as
     * using Jms Client Acknowledge.
     */
    public boolean isTransacted()
    {
        return true;
    }
}
