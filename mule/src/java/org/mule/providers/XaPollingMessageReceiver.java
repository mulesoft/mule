/*
 * $Header$ $Revision$ $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Cubis Limited. All rights reserved. http://www.cubis.co.uk
 * 
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *  
 */

package org.mule.providers;

import org.mule.InitialisationException;
import org.mule.MuleManager;
import org.mule.transaction.XaTransaction;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOConnector;
import org.mule.util.timer.TimeEventListener;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

/**
 * <p/>
 * <code>XaPollingMessageReceiver</code> is a polling Message receiver that manages
 * an XA transaction for the received message
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public abstract class XaPollingMessageReceiver extends PollingMessageReceiver implements TimeEventListener
{
    private TransactionManager transactionManager;
    private Transaction transaction;

    public XaPollingMessageReceiver(UMOConnector connector,
                                    UMOComponent component,
                                    UMOEndpoint endpoint,
                                    Long frequency)
            throws InitialisationException
    {
        super(connector, component, endpoint, frequency);
        transactionManager = MuleManager.getInstance().getTransactionManager();
        if (transactionManager == null) {
            throw new InitialisationException("A transaction manager must be configured for the Mule server");
        }
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.util.timer.TimeEventListener#timeExpired(org.mule.util.timer.TimeEvent)
	 */
    public synchronized void poll()
    {
        try {
            startTransaction();
        }
        catch (Exception e) {
            handleException("Caught exception trying to start transaction. This thread will terminate. Reason: " + e, e);
        }
        try {
            Object message = getMessage();
            if (message != null) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Received Message: " + message);
                }
                processMessage(message, new XaTransaction(transaction));
            }
            else {
                cancelTransaction();
            }
        }
        catch (Exception e1) {
            logger.error(e1.getMessage(), e1);
            try {
                rollbackTransaction();
                endpoint.getConnector().stop();
            }
            catch (Exception e) {
                logger.error("Failed to stop endpoint: " + e, e);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.mule.providers.XaPollingMessageReceiver#getXAResource()
     */
    protected abstract XAResource getXAResource() throws Exception;

    protected abstract Object getMessage();

    protected abstract void processMessage(Object message, XaTransaction transaction) throws UMOException;

    protected void enlist(Transaction transaction) throws Exception
    {
        transaction.enlistResource(getXAResource());
    }

    /**
     * Delists any resources from the current transaction. This includes the
     * current input Messenger's Session as well as any resources used by the
     * MessageListener if it implements XACapable
     *
     * @param transaction
     * @param flag        is the flag used by JTA when delisting resources. It is
     *                    either XAResource.TMSUCCESS, XAResource.TMSUSPEND, or
     *                    XAResource.TMFAIL
     * @throws Exception
     */
    protected void delist(Transaction transaction, int flag) throws Exception
    {
        transaction.delistResource(getXAResource(), flag);
    }

    /**
     * Strategy method to represent the code required to start a transaction.
     */
    protected void startTransaction() throws Exception
    {
        while(transactionManager.getTransaction()!=null) {
            wait(100);
        }
        logger.debug("starting Xa Transaction");
        transactionManager.begin();
        transaction = transactionManager.getTransaction();
        enlist(transaction);
    }

    /**
     * Strategy method to represent the code required to commit a transaction.
     */
    protected void commitTransaction() throws Exception
    {
        delist(transaction, XAResource.TMSUCCESS);
        try {
            logger.debug("Committing Xa Transaction");
            transaction.commit();
        }
        catch (Exception e) {
            logger.error("Caught exception while committing txn: " + e, e);
            transaction.setRollbackOnly();
            throw e;
        }
    }

    /**
     * Strategy method to represent the code required to rollback a
     * transaction.
     */
    protected void rollbackTransaction() throws Exception
    {
        delist(transaction, XAResource.TMFAIL);
        logger.debug("rolling back Xa Transaction");        
        transaction.rollback();
    }

    /**
     * Strategy method to represent the code required to cancel a transaction.
     * This is called when a message is not received.
     */
    protected void cancelTransaction() throws Exception
    {
        delist(transaction, XAResource.TMFAIL);
        transaction.rollback();
    }
}
