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

import javax.sql.DataSource;

import org.mule.transaction.AbstractSingleResourceTransaction;
import org.mule.transaction.IllegalTransactionStateException;
import org.mule.transaction.TransactionRollbackException;
import org.mule.umo.UMOTransactionException;

/**
 * @author Guillaume Nodet
 * @version $Revision$
 */
public class JdbcTransaction extends AbstractSingleResourceTransaction {

	public JdbcTransaction() {
	}
	
	/* (non-Javadoc)
	 * @see org.mule.umo.UMOTransaction#bindResource(java.lang.Object, java.lang.Object)
	 */
	public void bindResource(Object key, Object resource) throws UMOTransactionException {
		if (!(key instanceof DataSource) || !(resource instanceof Connection)) {
			throw new IllegalTransactionStateException("Can only bind javax.sql.DataSource/java.sql.Connection resources");
		}
		Connection con = (Connection) resource;
		try {
			if (con.getAutoCommit()) {
				con.setAutoCommit(false);
			}
		} catch (SQLException e) {
			throw new UMOTransactionException("Could not set autoCommit", e);
		}
		super.bindResource(key, resource);
	}

	/* (non-Javadoc)
	 * @see org.mule.transaction.AbstractSingleResourceTransaction#doBegin()
	 */
	protected void doBegin() throws UMOTransactionException {
		// Do nothing
	}

	/* (non-Javadoc)
	 * @see org.mule.transaction.AbstractSingleResourceTransaction#doCommit()
	 */
	protected void doCommit() throws UMOTransactionException {
		try {
			((Connection) resource).commit();
			((Connection) resource).close();
		} catch (SQLException e) {
			throw new UMOTransactionException("Could not commit transaction", e);
		}
	}

	/* (non-Javadoc)
	 * @see org.mule.transaction.AbstractSingleResourceTransaction#doRollback()
	 */
	protected void doRollback() throws UMOTransactionException {
		try {
			((Connection) resource).rollback();
			((Connection) resource).close();
		} catch (SQLException e) {
			throw new TransactionRollbackException("Could not rollback transaction", e);
		}
	}

}
