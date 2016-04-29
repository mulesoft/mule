/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionException;
import org.mule.runtime.core.api.transaction.UniversalTransactionFactory;

/**
 * <p>
 * <code>JmsTransactionFactory</code> creates a JMS local transaction
 */
public class JmsTransactionFactory implements UniversalTransactionFactory
{
    private String name;
    
    @Override
    public Transaction beginTransaction(MuleContext muleContext) throws TransactionException
    {
        JmsTransaction tx = new JmsTransaction(muleContext);
        tx.begin();
        return tx;
    }

    @Override
    public boolean isTransacted()
    {
        return true;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public Transaction createUnboundTransaction(MuleContext muleContext)
    {
        return new JmsTransaction(muleContext);
    }
}
