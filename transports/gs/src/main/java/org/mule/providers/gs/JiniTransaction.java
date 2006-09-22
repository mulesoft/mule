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

import com.j_spaces.core.IJSpace;
import com.j_spaces.core.client.LocalTransactionManager;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionFactory;
import net.jini.core.transaction.server.TransactionManager;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.providers.gs.space.GSSpace;
import org.mule.transaction.AbstractSingleResourceTransaction;
import org.mule.transaction.IllegalTransactionStateException;
import org.mule.transaction.TransactionRollbackException;
import org.mule.umo.TransactionException;

import java.rmi.RemoteException;

/**
 * Provides a Jini Transaction wrapper so that Jini transactions can be used in Mule.
 * As Jini does not use the standard JTA Transaction manager, the Jini TransactionManager
 * Must be set on the JiniTransactionFactory before any transactions are begun.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 * @see TransactionManager
 * @see JiniTransactionFactory
 */
public class JiniTransaction extends AbstractSingleResourceTransaction {

    protected TransactionManager txManager;
    protected long timeout;
    protected boolean unbound = true;

    public JiniTransaction(long timeout) {
        this.timeout = timeout;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.UMOTransaction#bindResource(java.lang.Object,
     *      java.lang.Object)
     */
    public void bindResource(Object key, Object resource) throws TransactionException {

        //We can only start the transaction when its bound as we need the Space to obtain the TransactionManager
        //Todo find a clean way of passing the Trsnasaction manager to the TransactionFactory
        try {
            txManager = LocalTransactionManager.getInstance((IJSpace) ((GSSpace) resource).getJavaSpace());
        } catch (RemoteException e) {
            throw new TransactionException(e);
        }
        try {
            Transaction.Created tCreated = TransactionFactory.create(txManager, timeout);
            Transaction transaction = tCreated.transaction;
            super.bindResource(resource, transaction);
            unbound = false;
        } catch (Exception e) {
            throw new IllegalTransactionStateException(new Message(Messages.TX_CANT_START_X_TRANSACTION, "Jini"), e);
        }

    }

    /**
     * Begin the transaction.
     *
     * @throws org.mule.umo.TransactionException
     *
     */
    public void doBegin() throws TransactionException {
        //Do nothing here, the transacted cannot be created until it is bound
        unbound = true;
    }


    /*
    * (non-Javadoc)
    *
    * @see org.mule.umo.UMOTransaction#commit()
    */
    protected void doCommit() throws TransactionException {
        if (unbound) {
            return;
        }
        try {
            ((Transaction) resource).commit();
        } catch (Exception e) {
            throw new IllegalTransactionStateException(new Message(Messages.TX_COMMIT_FAILED), e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.UMOTransaction#rollback()
     */
    protected void doRollback() throws TransactionException {
        try {
            if (unbound) {
                return;
            }
            ((Transaction) resource).abort();
        } catch (Exception e) {
            throw new TransactionRollbackException(e);
        }
    }
}
