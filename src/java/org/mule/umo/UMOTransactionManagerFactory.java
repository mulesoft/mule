/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk 
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.umo;

import javax.transaction.TransactionManager;

/**
 * <code>TranactionManagerFactory</code> is a factory class for creating a transaction
 * manager for the MuleServer.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
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
