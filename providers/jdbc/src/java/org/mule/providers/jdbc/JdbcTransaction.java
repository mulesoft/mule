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
package org.mule.providers.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.transaction.IllegalTransactionStateException;
import org.mule.transaction.TransactionRollbackException;
import org.mule.transaction.TransactionStatusException;
import org.mule.umo.UMOTransaction;
import org.mule.umo.UMOTransactionException;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

/**
 * @author Guillaume Nodet
 * @version $Revision$
 */
public class JdbcTransaction implements UMOTransaction {

	/** logger used by this class */
    protected static final transient Log logger = LogFactory.getLog(JdbcTransaction.class);

    public static final int STATUS_NO_TRANSACTION = 6;
    public static final int STATUS_COMMITTED = 3;
    public static final int STATUS_ROLLED_BACK = 4;
    public static final int STATUS_ACTIVE = 0;
	
	private Connection connection;
    private SynchronizedBoolean started = new SynchronizedBoolean(false);
    private SynchronizedBoolean committed = new SynchronizedBoolean(false);
    private SynchronizedBoolean rolledBack = new SynchronizedBoolean(false);
    private SynchronizedBoolean rollbackOnly = new SynchronizedBoolean(false);

	public JdbcTransaction(Connection connection) {
		this.connection = connection;
	}
	
	/* (non-Javadoc)
	 * @see org.mule.umo.UMOTransaction#begin()
	 */
	public void begin() throws UMOTransactionException {
		try {
			if (connection.getAutoCommit()) {
				connection.setAutoCommit(false);
			}
	        logger.debug("Beginning transaction");
	        started.commit(false, true);
		} catch (SQLException e) {
            throw new IllegalTransactionStateException("Failed to read set Jdbc connection automcommit to false: " + e.getMessage(), e);
		}
	}

	/* (non-Javadoc)
	 * @see org.mule.umo.UMOTransaction#commit()
	 */
	public void commit() throws UMOTransactionException {
        try {
            logger.debug("Committing transaction");
            if (rollbackOnly.get()) {
                throw new TransactionRollbackException("Transaction is marked for rollback only");
            }
            connection.commit();
            committed.commit(false, true);
        } catch (SQLException e) {
            throw new IllegalTransactionStateException("Failed to commit jdbc transaction: " + e.getMessage(), e);
        }
	}

	/* (non-Javadoc)
	 * @see org.mule.umo.UMOTransaction#rollback()
	 */
	public void rollback() throws TransactionRollbackException {
        try {
            logger.debug("Rolling back transaction");
            setRollbackOnly();
            connection.rollback();
            rolledBack.commit(false, true);
        } catch (SQLException e) {
            throw new TransactionRollbackException("Failed to rollback jdbc transaction: " + e.getMessage(), e);
        }
	}

	/* (non-Javadoc)
	 * @see org.mule.umo.UMOTransaction#getStatus()
	 */
	public int getStatus() throws TransactionStatusException {
        if (rolledBack.get()) return STATUS_ROLLED_BACK;
        if (committed.get()) return STATUS_COMMITTED;
        if (started.get()) return STATUS_ACTIVE;
        return STATUS_NO_TRANSACTION;
	}

	/* (non-Javadoc)
	 * @see org.mule.umo.UMOTransaction#isBegun()
	 */
	public boolean isBegun() throws TransactionStatusException {
        return started.get();
	}

	/* (non-Javadoc)
	 * @see org.mule.umo.UMOTransaction#isRolledBack()
	 */
	public boolean isRolledBack() throws TransactionStatusException {
        return rolledBack.get();
	}

	/* (non-Javadoc)
	 * @see org.mule.umo.UMOTransaction#isCommitted()
	 */
	public boolean isCommitted() throws TransactionStatusException {
        return committed.get();
	}

	/* (non-Javadoc)
	 * @see org.mule.umo.UMOTransaction#getResource()
	 */
	public Object getResource() {
		return connection;
	}

	/* (non-Javadoc)
	 * @see org.mule.umo.UMOTransaction#setRollbackOnly()
	 */
	public void setRollbackOnly() {
        rollbackOnly.set(true);
	}

	/* (non-Javadoc)
	 * @see org.mule.umo.UMOTransaction#isRollbackOnly()
	 */
	public boolean isRollbackOnly() {
        return rollbackOnly.get();
	}

}
