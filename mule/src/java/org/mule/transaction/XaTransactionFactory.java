/*
 * $Header$ 
 * $Revision$ 
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *  
 */

package org.mule.transaction;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.TransactionException;
import org.mule.umo.UMOTransaction;
import org.mule.umo.UMOTransactionFactory;

/**
 * <code>XaTransactionFactory</code> Is used to create/retreive a Transaction
 * from a transaction manager configured on the MuleManager.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @author Guillaume Nodet
 * @version $Revision$
 */
public class XaTransactionFactory implements UMOTransactionFactory
{
    public XaTransactionFactory()
    {
    }

    public UMOTransaction beginTransaction() throws TransactionException
    {
    	try {
        	XaTransaction xat = new XaTransaction();
        	xat.begin();
        	return xat;
    	} catch (Exception e) {
    		throw new TransactionException(new Message(Messages.TX_CANT_START_X_TRANSACTION, "XA"), e);
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