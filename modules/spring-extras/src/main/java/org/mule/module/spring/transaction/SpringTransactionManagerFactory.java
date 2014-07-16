/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.spring.transaction;

import org.mule.api.config.MuleConfiguration;
import org.mule.api.transaction.TransactionManagerFactory;

import javax.transaction.TransactionManager;

/**
 * A holder to a transaction manager set via a Spring Application context
 */
public class SpringTransactionManagerFactory implements TransactionManagerFactory
{

    private TransactionManager transactionManager;

    synchronized public void setTransactionManager(TransactionManager transactionManager)
    {
        this.transactionManager = transactionManager;
    }

    synchronized public TransactionManager create(MuleConfiguration config) throws Exception
    {
        return transactionManager;
    }

}
