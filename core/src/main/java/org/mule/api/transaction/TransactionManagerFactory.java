/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.transaction;

import org.mule.api.config.MuleConfiguration;

import javax.transaction.TransactionManager;

/**
 * <code>TranactionManagerFactory</code> is a factory class for creating a
 * transaction manager for the MuleServer.
 * 
 */
public interface TransactionManagerFactory
{
    /**
     * Creates of obtains the jta transaction manager to use for mule transactions
     * 
     * @return the transaction manager to use
     * @throws Exception if the transaction manager cannot be located or created
     * @param config Mule configuration parameters
     */
    TransactionManager create(MuleConfiguration config) throws Exception;
}
