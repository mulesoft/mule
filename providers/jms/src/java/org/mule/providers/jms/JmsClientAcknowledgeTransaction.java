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
import javax.jms.Message;
import javax.jms.Session;

import org.mule.transaction.AbstractSingleResourceTransaction;
import org.mule.transaction.IllegalTransactionStateException;
import org.mule.umo.UMOTransactionException;

/**
 * <p><code>JmsClientAcknowledgeTransaction</code> is a transaction implementation of performing
 * a message acknowledgement.  There is no notion of rollback with client acknowledgement, but this
 * transaction can be useful for controlling how messages are consumed from a destination.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @author Guillaume Nodet
 * @version $Revision$
 */
public class JmsClientAcknowledgeTransaction extends AbstractSingleResourceTransaction
{
	private Message message;
	
	/* (non-Javadoc)
	 * @see org.mule.transaction.AbstractSingleResourceTransaction#doBegin()
	 */
	protected void doBegin() throws UMOTransactionException {
		// nothing to do
	}

	/* (non-Javadoc)
	 * @see org.mule.transaction.AbstractSingleResourceTransaction#doCommit()
	 */
	protected void doCommit() throws UMOTransactionException {
		try {
			if (message == null) {
				throw new IllegalTransactionStateException("No message has been bound for acknowledgement");
			}
			message.acknowledge();
		} catch (JMSException e) {
            throw new IllegalTransactionStateException("Failed to commit jms Client Acknowledge transaction: " + e.getMessage(), e);
		}
	}

	/* (non-Javadoc)
	 * @see org.mule.transaction.AbstractSingleResourceTransaction#doRollback()
	 */
	protected void doRollback() throws UMOTransactionException {
		// If a message has been bound, rollback is forbidden
		if (message != null) {
			throw new UnsupportedOperationException("Jms Client Acknowledge doesn't support rollback");
		}
	}

	/* (non-Javadoc)
	 * @see org.mule.umo.UMOTransaction#bindResource(java.lang.Object, java.lang.Object)
	 */
	public void bindResource(Object key, Object resource) throws UMOTransactionException {
		if (key instanceof Message) {
			this.message = (Message) key;
			return;
		}
		if (!(key instanceof Connection) || !(resource instanceof Session)) {
			throw new IllegalTransactionStateException("Can only bind javax.sql.DataSource/java.sql.Connection resources");
		}
		Session session = (Session) resource;
		try {
			if (session.getTransacted()) {
				throw new IllegalTransactionStateException("Jms session should not be transacted");
			}
		} catch (JMSException e) {
			throw new IllegalTransactionStateException("Could not retrieve transacted state for session", e);
		}
		super.bindResource(key, resource);
	}
}
