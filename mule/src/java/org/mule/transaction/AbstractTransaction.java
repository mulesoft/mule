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
package org.mule.transaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.umo.UMOTransaction;
import org.mule.umo.UMOTransactionException;

/**
 * This base class provides low level features for transactions
 * 
 * @author Guillaume Nodet
 * @version $Revision$
 */
public abstract class AbstractTransaction implements UMOTransaction {

	protected final transient Log logger = LogFactory.getLog(getClass());
	
	/* (non-Javadoc)
	 * @see org.mule.umo.UMOTransaction#isRollbackOnly()
	 */
	public boolean isRollbackOnly() throws UMOTransactionException {
        return getStatus() == STATUS_MARKED_ROLLBACK;
	}

	/* (non-Javadoc)
	 * @see org.mule.umo.UMOTransaction#isBegun()
	 */
	public boolean isBegun() throws UMOTransactionException {
		int status = getStatus();
		return status != STATUS_NO_TRANSACTION && status != STATUS_UNKNOWN;
	}

	/* (non-Javadoc)
	 * @see org.mule.umo.UMOTransaction#isRolledBack()
	 */
	public boolean isRolledBack() throws UMOTransactionException {
        return getStatus() == STATUS_ROLLEDBACK;
	}

	/* (non-Javadoc)
	 * @see org.mule.umo.UMOTransaction#isCommitted()
	 */
	public boolean isCommitted() throws UMOTransactionException {
		return getStatus() == STATUS_COMMITTED;
	}

	/* (non-Javadoc)
	 * @see org.mule.umo.UMOTransaction#begin()
	 */
	public void begin() throws UMOTransactionException {
        logger.debug("Beginning transaction");
        doBegin();
        TransactionCoordination.getInstance().bindTransaction(this);
	}

	/* (non-Javadoc)
	 * @see org.mule.umo.UMOTransaction#commit()
	 */
	public void commit() throws UMOTransactionException {
        logger.debug("Committing transaction");
        if (isRollbackOnly()) {
        	throw new IllegalTransactionStateException("Transaction is marked for rollback");
        }
	    doCommit();
		TransactionCoordination.getInstance().unbindTransaction(this);
	}

	/* (non-Javadoc)
	 * @see org.mule.umo.UMOTransaction#rollback()
	 */
	public void rollback() throws UMOTransactionException {
		try {
	        logger.debug("Rolling back transaction");
	        setRollbackOnly();
	        doRollback();
		} finally {
			TransactionCoordination.getInstance().unbindTransaction(this);
		}
	}

	/**
	 * Really begin the transaction.
	 * Note that resources are enlisted yet. 
	 * @throws UMOTransactionException
	 */
	protected abstract void doBegin() throws UMOTransactionException;

	/**
	 * Commit the transaction on the underlying resource 
	 * @throws UMOTransactionException
	 */
	protected abstract void doCommit() throws UMOTransactionException;

	/**
	 * Rollback the transaction on the underlying resource 
	 * @throws UMOTransactionException
	 */
	protected abstract void doRollback() throws UMOTransactionException;
	
}
