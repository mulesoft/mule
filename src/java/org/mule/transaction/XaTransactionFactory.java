/*
 * $Header$ 
 * $Revision$ 
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Cubis Limited. All rights reserved. http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *  
 */

package org.mule.transaction;

import org.mule.umo.UMOTransaction;
import org.mule.umo.UMOTransactionException;
import org.mule.umo.UMOTransactionFactory;

/**
 * <code>XaTransactionFactory</code> Is used to create/retreive a Transaction
 * from a transaction manager configured on the MuleManager.
 * 
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @author Guillaume Nodet
 * @version $Revision$
 */
public class XaTransactionFactory implements UMOTransactionFactory
{
    public XaTransactionFactory()
    {
    }

    public UMOTransaction beginTransaction() throws UMOTransactionException
    {
    	try {
        	XaTransaction xat = new XaTransaction();
        	xat.begin();
        	return xat;
    	} catch (Exception e) {
    		throw new UMOTransactionException("Can not start xa transaction", e);
    	}
    }

    /**
     * Determines whether this transaction factory creates transactions that are really
     * transacted or if they are being used to
     * simulate batch actions, such as using Jms Client Acknowledge.
     *
     * @return
     */
    public boolean isTransacted()
    {
        return true;
    }
}