/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.testmodels.mule;

import org.mule.api.MuleContext;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.api.transaction.TransactionFactory;

/**
 * <code>TestTransactionFactory</code> creates a {@link org.mule.tck.testmodels.mule.TestTransaction}
 * 
 */

public class TestTransactionFactory implements TransactionFactory
{

    // for testsing properties
    private String value;
    private Transaction mockTransaction;

    public TestTransactionFactory()
    {
    }

    public TestTransactionFactory(Transaction mockTransaction)
    {
        this.mockTransaction = mockTransaction;
    }



    public Transaction beginTransaction(MuleContext muleContext) throws TransactionException
    {
        Transaction testTransaction;
        if (mockTransaction != null)
        {
            testTransaction = mockTransaction;
        }
        else
        {
            testTransaction = new TestTransaction(muleContext);
        }

        testTransaction.begin();
        return testTransaction;
    }

    public boolean isTransacted()
    {
        return true;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

}
