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


/**
 * <p><code>UMOTransactionFactory</code> creates a transaction.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public interface UMOTransactionFactory
{
    /**
     * Create and begins a new transaction
     * @param session can be any object used to manage the transaction
     * @return a new Transaction
     * @throws UMOTransactionException if the transaction cannot be created or begun
     */
    public UMOTransaction beginTransaction(Object session) throws UMOTransactionException;

    /**
     * Determines whether this transaction factory creates transactions that are really
     * transacted or if they are being used to
     * simulate batch actions, such as using Jms Client Acknowledge.
     * @return
     */
    public boolean isTransacted();
}
