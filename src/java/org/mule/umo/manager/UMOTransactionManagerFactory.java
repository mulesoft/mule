/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.umo.manager;

import javax.transaction.TransactionManager;

/**
 * <code>TranactionManagerFactory</code> is a factory class for creating a transaction
 * manager for the MuleServer.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface UMOTransactionManagerFactory
{
    /**
     * Creates of obtains the jta transaction manager to use for mule transactions
     * @return the transaction manager to use
     * @throws Exception if the transaction manager cannot be located or created
     */
    public TransactionManager create() throws Exception;
}
