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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.transaction.constraints.ConstraintFilter;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOTransaction;
import org.mule.umo.UMOTransactionException;

/**
 * <code>TransactionProxy</code> TODO (document class)
 * 
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class TransactionProxy implements UMOTransaction
{
    /** logger used by this class */
    protected static final transient Log logger = LogFactory.getLog(TransactionProxy.class);

    private UMOTransaction transaction;
    private ConstraintFilter constraint;

    public TransactionProxy(UMOTransaction transaction, ConstraintFilter contraint)
    {
        this.transaction = transaction;
        this.constraint = contraint;
    }

    public void begin() throws UMOTransactionException
    {
        transaction.begin();
        TransactionCoordination.getInstance().bindTransaction(transaction, constraint);
    }

    public final void commit() throws UMOTransactionException
    {
        throw new UMOTransactionException("commit() method not supported on proxy. USe commit(UMOEvent) instead");
    }

    public void commit(UMOEvent event) throws UMOTransactionException
    {
        if(transaction.isRollbackOnly())
        {
            throw new IllegalTransactionStateException("Cannot commit transaction as it is marked for rollback only");
        }
        else if(canCommit(event)) {
            transaction.commit();
            TransactionCoordination.getInstance().unbindTransaction();
        } else if(TransactionCoordination.getInstance().getTransaction()==null)
        {
            //rebind the transaction if it was unbound
            //this shouldm never happen, but it could occur is user code was
            //managing transaction demarcation
//            logger.warn("rebinding transaction");
//            TransactionCoordination.getInstance().bindTransaction(transaction, constraints);
            throw new IllegalTransactionStateException("Atransaction is in progress but no Transaction is bound in the TransactionCoordination");
        }
    }

    public Object getResource()
    {
        return transaction.getResource();
    }

    public int getStatus() throws TransactionStatusException
    {
        return transaction.getStatus();
    }

    public boolean isBegun() throws TransactionStatusException
    {
        return transaction.isBegun();
    }

    public boolean isCommitted() throws TransactionStatusException
    {
        return transaction.isCommitted();
    }

    public boolean isRollbackOnly()
    {
        return transaction.isRollbackOnly();
    }

    public boolean isRolledBack() throws TransactionStatusException
    {
        return transaction.isRolledBack();
    }

    public void rollback() throws TransactionRollbackException
    {
        transaction.rollback();
        TransactionCoordination.getInstance().unbindTransaction();
    }

    public void setRollbackOnly()
    {
        transaction.setRollbackOnly();
    }

    public UMOTransaction getTransaction()
    {
        return transaction;
    }

    public ConstraintFilter getConstraint()
    {
        return constraint;
    }

    public void setTransaction(UMOTransaction transaction)
    {
        this.transaction = transaction;
    }

    public boolean canCommit(UMOEvent event)
    {
        if(constraint!=null) {
            return constraint.accept(event) && !transaction.isRollbackOnly();
        } else {
            return !transaction.isRollbackOnly();
        }
    }
}