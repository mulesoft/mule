/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.spring.transaction;

import org.mule.umo.manager.UMOTransactionManagerFactory;

import javax.transaction.TransactionManager;

/**
 * A holder to a transaction manager set via a Spring Application context
 */
public class SpringTransactionManagerFactory implements UMOTransactionManagerFactory
{

    private TransactionManager transactionManager;

    synchronized public void setTransactionManager(TransactionManager transactionManager)
    {
        this.transactionManager = transactionManager;
    }

    synchronized public TransactionManager create() throws Exception
    {
        return transactionManager;
    }

}
