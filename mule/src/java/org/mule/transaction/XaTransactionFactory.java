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

import org.mule.MuleManager;
import org.mule.umo.UMOTransaction;
import org.mule.umo.UMOTransactionException;
import org.mule.umo.UMOTransactionFactory;
import org.mule.util.ClassHelper;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import java.lang.reflect.Method;

/**
 * <code>XaTransactionFactory</code> Is used to create/retreive a Transaction
 * from a transaction manager configured on the MuleManager.
 * 
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class XaTransactionFactory implements UMOTransactionFactory
{
    TransactionManager txManager;
    public XaTransactionFactory()
    {
        txManager = MuleManager.getInstance().getTransactionManager();
        if(txManager == null)
        {
            throw new IllegalStateException("A transaction manager must be set on the Mule Manager");
        }
    }

    public UMOTransaction beginTransaction(Object session) throws UMOTransactionException
    {
        XaTransaction tx = null;

        if(session instanceof XAResource)
        {
            tx = getTransaction((XAResource)session);
        } else {
            Method method = ClassHelper.getMethod("getXAResource", session.getClass());
            if(method== null)
            {
                throw new IllegalTransactionStateException("Failed to obtain XA resource from session: " + session.getClass().getName());
            } else {
                Object resource = null;
                try {
                    resource = method.invoke(session, (Object[])null);
                }
                catch (Exception e) {
                    throw new IllegalTransactionStateException("Could not obtain XAResource from session calling getXAResource: " + session.getClass().getName());
                }

                if(resource != null && resource instanceof XAResource)
                {
                    tx = getTransaction((XAResource)resource);
                } else {
                    throw new IllegalTransactionStateException("Could not obtain XAResource from session: " + session.getClass().getName());
                }
            }
        }
        tx.begin();
        return tx;
    }

    private XaTransaction getTransaction(XAResource resource) throws UMOTransactionException
    {
        try {
                Transaction tx = txManager.getTransaction();
                tx.enlistResource(resource);
                return new XaTransaction(tx);
            }
            catch (SystemException e) {
                throw new UMOTransactionException("Failed to create XA transaction: " + e, e);
            }
            catch (RollbackException e) {
                throw new TransactionRollbackException(e.getMessage(), e);
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