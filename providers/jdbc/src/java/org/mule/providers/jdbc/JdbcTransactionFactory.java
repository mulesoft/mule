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

import org.mule.transaction.IllegalTransactionStateException;
import org.mule.umo.UMOTransaction;
import org.mule.umo.UMOTransactionException;
import org.mule.umo.UMOTransactionFactory;

/**
 * @author Guillaume Nodet
 * @version $Revision$
 */
public class JdbcTransactionFactory implements UMOTransactionFactory {

	/* (non-Javadoc)
	 * @see org.mule.umo.UMOTransactionFactory#beginTransaction(java.lang.Object)
	 */
	public UMOTransaction beginTransaction(Object session) throws UMOTransactionException {
		if (session instanceof Connection) {
            JdbcTransaction tx = new JdbcTransaction((Connection) session);
            tx.begin();
            return tx;
		} else {
            throw new IllegalTransactionStateException("Session was not of expected type: " + Connection.class.getName());
		}
	}

	/* (non-Javadoc)
	 * @see org.mule.umo.UMOTransactionFactory#isTransacted()
	 */
	public boolean isTransacted() {
		return true;
	}

}
