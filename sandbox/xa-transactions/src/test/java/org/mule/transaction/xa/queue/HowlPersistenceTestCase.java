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

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.objectweb.howl.log.Configuration;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class HowlPersistenceTestCase extends AbstractTransactionQueueManagerTestCase {

	private static final Log logger = LogFactory.getLog(HowlPersistenceTestCase.class);
	
	protected static final String STORE_DIR = "./target/howl";
	
	protected void deleteWholeDir(File f) {
		if (f.exists()) {
			File[] files = f.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteWholeDir(files[i]);
				} else {
					logger.info("Deleting " + files[i]);
					files[i].delete();
				}
			}
			f.delete();
		}
	}
	
	protected void setUp() throws Exception {
		deleteWholeDir(new File(STORE_DIR));
	}
	
	protected Log getLogger() {
		return logger;
	}
	
	protected TransactionalQueueManager createQueueManager() throws Exception {
		TransactionalQueueManager mgr = new TransactionalQueueManager();
		Configuration cfg = new Configuration();
		cfg.setLogFileDir(new File(STORE_DIR).getCanonicalPath());
		cfg.setBufferSize(32);
		cfg.setMaxBlocksPerFile(64);
		cfg.setFlushSleepTime(2000);
		mgr.setPersistenceStrategy(new HowlPersistenceStrategy(cfg));
		return mgr;
	}

	/* (non-Javadoc)
	 * @see org.mule.transaction.xa.queue.AbstractTransactionQueueManagerTestCase#isPersistent()
	 */
	protected boolean isPersistent() {
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		HowlPersistenceTestCase test = new HowlPersistenceTestCase();
		//test.setUp();
		test.testBench();
	}
	
}
