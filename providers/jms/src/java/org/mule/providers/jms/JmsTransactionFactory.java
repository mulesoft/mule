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

package org.mule.providers.jms;

import org.mule.transaction.IllegalTransactionStateException;
import org.mule.umo.UMOTransaction;
import org.mule.umo.UMOTransactionException;
import org.mule.umo.UMOTransactionFactory;

import javax.jms.Session;

/**
 * <p><code>JmsTransactionFactory</code> TODO (document class)
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class JmsTransactionFactory implements UMOTransactionFactory
{
    /* (non-Javadoc)
     * @see org.mule.umo.UMOTransactionFactory#beginTransaction(org.mule.umo.provider.UMOMessageDispatcher)
     */
    public UMOTransaction beginTransaction(Object session) throws UMOTransactionException
    {
        if (session instanceof Session)
        {
            JmsTransaction tx = new JmsTransaction((Session) session);
            tx.begin();
            return tx;
        }
        else
        {
            throw new IllegalTransactionStateException("Session was not of expected type: " + Session.class.getName());
        }
    }

    public boolean isTransacted()
    {
        return true;
    }

}
