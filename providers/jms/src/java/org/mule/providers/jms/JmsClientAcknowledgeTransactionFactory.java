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
 */
package org.mule.providers.jms;

import org.mule.umo.UMOTransaction;
import org.mule.umo.UMOTransactionException;
import org.mule.umo.UMOTransactionFactory;

/**
 * <code>JmsClientAcknowledgeTransactionFactory</code> creates a Jms Client Acknowledge transaction
 * using a Jms message.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @author Guillaume Nodet
 * @version $Revision$
 */

public class JmsClientAcknowledgeTransactionFactory implements UMOTransactionFactory
{
    /* (non-Javadoc)
     * @see org.mule.umo.UMOTransactionFactory#beginTransaction(java.lang.Object)
     */
    public UMOTransaction beginTransaction() throws UMOTransactionException
    {
        JmsClientAcknowledgeTransaction tx = new JmsClientAcknowledgeTransaction();
        tx.begin();
        return tx;
    }

    public boolean isTransacted()
    {
        return false;
    }
}