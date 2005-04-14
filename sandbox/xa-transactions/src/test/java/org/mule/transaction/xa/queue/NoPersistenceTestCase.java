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
package org.mule.transaction.xa.queue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class NoPersistenceTestCase extends
		AbstractTransactionQueueManagerTestCase {

	private static final Log logger = LogFactory.getLog(FilePersistenceTestCase.class);
	
	protected Log getLogger() {
		return logger;
	}
	
	/* (non-Javadoc)
	 * @see org.mule.transaction.xa.queue.AbstractTransactionQueueManagerTestCase#createQueueManager()
	 */
	protected TransactionalQueueManager createQueueManager() throws Exception {
		TransactionalQueueManager mgr = new TransactionalQueueManager();
		return mgr;
	}

	/* (non-Javadoc)
	 * @see org.mule.transaction.xa.queue.AbstractTransactionQueueManagerTestCase#isPersistent()
	 */
	protected boolean isPersistent() {
		return false;
	}

}
