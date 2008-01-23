/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transaction;

import org.mule.api.MuleContext;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.api.transaction.TransactionFactory;
import org.mule.config.i18n.CoreMessages;

/**
 * <code>XaTransactionFactory</code> Is used to create/retrieve a Transaction from
 * a transaction manager configured on the MuleManager.
 */
public class XaTransactionFactory implements TransactionFactory
{
    public Transaction beginTransaction(MuleContext muleContext) throws TransactionException
    {
        try
        {
            XaTransaction xat = new XaTransaction(muleContext.getTransactionManager());
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
     * 
     * @return
     */
    public boolean isTransacted()
    {
        return true;
    }
}
