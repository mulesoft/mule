/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

    public Transaction beginTransaction(MuleContext muleContext) throws TransactionException
    {
        return new TestTransaction();
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
