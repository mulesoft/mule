/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
