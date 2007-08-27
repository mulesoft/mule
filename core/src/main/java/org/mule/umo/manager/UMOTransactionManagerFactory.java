/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.manager;

import javax.transaction.TransactionManager;

/**
 * <code>TranactionManagerFactory</code> is a factory class for creating a
 * transaction manager for the MuleServer.
 * 
 */
public interface UMOTransactionManagerFactory
{
    /**
     * Creates of obtains the jta transaction manager to use for mule transactions
     * 
     * @return the transaction manager to use
     * @throws Exception if the transaction manager cannot be located or created
     */
    TransactionManager create() throws Exception;
}
