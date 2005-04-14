/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */
package org.mule.providers;

import org.mule.transaction.TransactionCallback;
import org.mule.transaction.TransactionTemplate;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;

import java.util.Iterator;
import java.util.List;

/**
 * @author Guillaume Nodet
 * @version $Revision$
 */
public abstract class TransactedPollingMessageReceiver extends
		PollingMessageReceiver {

	protected boolean receiveMessagesInTransaction = true;
	
	public TransactedPollingMessageReceiver() {
	}
	
    public TransactedPollingMessageReceiver(UMOConnector connector,
            UMOComponent component,
            final UMOEndpoint endpoint, Long frequency) throws InitialisationException {
    	super(connector, component, endpoint, frequency);
    }
	
    public void poll() throws Exception
    {
    	TransactionTemplate tt = new TransactionTemplate(endpoint.getTransactionConfig(), connector.getExceptionListener());
		if (receiveMessagesInTransaction) {
	    	TransactionCallback cb = new TransactionCallback() {
				public Object doInTransaction() throws Exception {
		            List messages = getMessages();
		            if (messages != null && messages.size() > 0) {
		            	for (Iterator it = messages.iterator(); it.hasNext();) {
		            		Object message = it.next();
			                if (logger.isTraceEnabled()) {
			                    logger.trace("Received Message: " + message);
			                }
			                processMessage(message);
		            	}
		            }
					return null;
				}
	    	};
	    	tt.execute(cb);
		} else {
            List messages = getMessages();
            if (messages != null) {
            	for (Iterator it = messages.iterator(); it.hasNext();) {
            		final Object message = it.next();
                    if (logger.isTraceEnabled()) {
                        logger.trace("Received Message: " + message);
                    }
					TransactionCallback cb = new TransactionCallback() {
						public Object doInTransaction() throws Exception {
		                    processMessage(message);
							return null;
						}
					};
					tt.execute(cb);
            	}
            }
		}
    }

    protected abstract List getMessages() throws Exception;

    protected abstract void processMessage(Object message) throws Exception;
}
