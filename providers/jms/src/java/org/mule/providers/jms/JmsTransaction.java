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

package org.mule.providers.jms;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;

import org.mule.transaction.AbstractSingleResourceTransaction;
import org.mule.transaction.IllegalTransactionStateException;
import org.mule.umo.UMOTransactionException;

/**
 * <p><code>JmsTransaction</code> is a wrapper for a Jms local transaction.  This object holds the
 * jms session and controls the when the transaction committed or rolled back.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @author Guillaume Nodet
 * @version $Revision$
 */
public class JmsTransaction extends AbstractSingleResourceTransaction
{

	/* (non-Javadoc)
	 * @see org.mule.umo.UMOTransaction#bindResource(java.lang.Object, java.lang.Object)
	 */
	public void bindResource(Object key, Object resource) throws UMOTransactionException {
		if (!(key instanceof Connection) || !(resource instanceof Session)) {
			throw new IllegalTransactionStateException("Can only bind javax.sql.DataSource/java.sql.Connection resources");
		}
		Session session = (Session) resource;
		try {
			if (!session.getTransacted()) {
				throw new IllegalTransactionStateException("Jms session should be transacted");
			}
		} catch (JMSException e) {
			throw new IllegalTransactionStateException("Could not retrieve transacted state for session", e);
		}
		super.bindResource(key, resource);
	}

	/* (non-Javadoc)
	 * @see org.mule.transaction.AbstractSingleResourceTransaction#doBegin()
	 */
	protected void doBegin() throws UMOTransactionException {
		// do nothing
	}

	/* (non-Javadoc)
	 * @see org.mule.transaction.AbstractSingleResourceTransaction#doCommit()
	 */
	protected void doCommit() throws UMOTransactionException {
		try {
			((Session) resource).commit();
		} catch (JMSException e) {
			throw new UMOTransactionException("Could not commit transaction", e);
		}
	}

	/* (non-Javadoc)
	 * @see org.mule.transaction.AbstractSingleResourceTransaction#doRollback()
	 */
	protected void doRollback() throws UMOTransactionException {
		try {
			((Session) resource).rollback();
		} catch (JMSException e) {
			throw new UMOTransactionException("Could not rollback transaction", e);
		}
	}

}
