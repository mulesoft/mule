/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.gs;

import net.jini.core.transaction.server.TransactionManager;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.TransactionException;
import org.mule.umo.UMOTransaction;
import org.mule.umo.UMOTransactionFactory;

/**
 * Creates a Jini Transaction. This factory needs to have the TransactionManager set
 * on it before the <code>beginTransaction</code> method is called on it. This
 * should be set during start up of the Mule server by the resource that will be
 * sending or receiving events from a JavaSpace.
 * 
 * @see TransactionManager
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class JiniTransactionFactory implements UMOTransactionFactory
{

    protected TransactionManager transactionManager = null;
    protected long transactionTimeout = 3200 * 1000;

    /**
     * Create and begins a new transaction
     * 
     * @return a new Transaction
     * @throws org.mule.umo.TransactionException if the transaction cannot be created
     *             or begun
     */
    public UMOTransaction beginTransaction() throws TransactionException
    {
        // if(transactionManager==null) {
        // throw new TransactionException(new Message(Messages.X_IS_NULL,
        // "transactionManager"));
        // }
        try
        {
            JiniTransaction jtx = new JiniTransaction(transactionTimeout);
            jtx.begin();
            return jtx;
        }
        catch (Exception e)
        {
            throw new TransactionException(new Message(Messages.TX_CANT_START_X_TRANSACTION, "Jini"), e);
        }
    }

    /**
     * Determines whether this transaction factory creates transactions that are
     * really transacted or if they are being used to simulate batch actions, such as
     * using Jms Client Acknowledge.
     * 
     * @return
     */
    public boolean isTransacted()
    {
        return true;
    }

    public TransactionManager getTransactionManager()
    {
        return transactionManager;
    }

    public void setTransactionManager(TransactionManager transactionManager)
    {
        this.transactionManager = transactionManager;
    }

    public long getTransactionTimeout()
    {
        return transactionTimeout;
    }

    public void setTransactionTimeout(long transactionTimeout)
    {
        this.transactionTimeout = transactionTimeout;
    }
}
