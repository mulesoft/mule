/*
 * Created on 7 mars 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.mule.test.integration.transaction;

import java.util.Map;

import javax.jms.Message;
import javax.jms.TextMessage;

import org.mule.transaction.TransactionCoordination;
import org.mule.umo.UMOTransaction;

/**
 * @author gnt
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class XABridgeComponent {
	
	public static boolean mayRollback = false;

	protected void mayRollback() throws Exception {
		if (mayRollback) {
			UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();
			if (tx != null) {
				if (Math.random() < 0.3) {
					System.err.println("Marking transaction for rollback");
					tx.setRollbackOnly();
				}
			}
		}
	}
	
	public Object onJdbcMessage(Map msg) throws Exception {
		mayRollback();
		return msg.get("data").toString();
	}
	
	public Object onJmsMessage(String msg) throws Exception {
		mayRollback();
		return msg;
	}

}
