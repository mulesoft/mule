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
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.transaction.xa.AbstractResourceManager;
import org.objectweb.howl.log.Configuration;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class BenchmarkTestCase extends TestCase {

	private static final Log logger = LogFactory.getLog(BenchmarkTestCase.class); 
	
	protected static final String FILE_DIR = "./target/file";
	protected static final String HOWL_DIR = "./target/howl";
	
	protected TransactionalQueueManager createFileQueueManager() throws Exception {
		TransactionalQueueManager mgr = new TransactionalQueueManager();
		mgr.setPersistenceStrategy(new FilePersistenceStrategy(new File(FILE_DIR)));
		return mgr;
	}

	protected TransactionalQueueManager createHowlQueueManager() throws Exception {
		TransactionalQueueManager mgr = new TransactionalQueueManager();
		Configuration cfg = new Configuration();
		cfg.setLogFileDir(new File(HOWL_DIR).getCanonicalPath());
		cfg.setBufferSize(32);
		cfg.setMaxBlocksPerFile(64);
		cfg.setFlushSleepTime(2000);
		mgr.setPersistenceStrategy(new HowlPersistenceStrategy(cfg));
		return mgr;
	}

	
	public void testBench() throws Exception {
		benchmark(createFileQueueManager());
		benchmark(createHowlQueueManager());
	}
	
	public static void main(String[] args) throws Exception {
		new BenchmarkTestCase().testBench();
	}
	
	protected void benchmark(TransactionalQueueManager mgr) throws Exception {
		logger.info("================================");
		logger.info("Running test: " + mgr.getPersistenceStrategy().getClass().getName());
		logger.info("================================");
		
		try {
			mgr.start();
		
			QueueSession s = mgr.getQueueSession();
			Queue q = s.getQueue("queue1");
			
			Random rnd = new Random();
			long t0 = System.currentTimeMillis();
			for (int i = 0; i < 10; i++) {
				for (int j = 0; j < 500; j++) {
					byte[] o = new byte[2048];
					rnd.nextBytes(o);
					q.put(o);
				}
				while (q.size() > 0) {
					q.take();
				}
			}
			long t1 = System.currentTimeMillis();
	
			logger.info("Time: " + (t1 - t0) + " ms");
			
			mgr.stop(AbstractResourceManager.SHUTDOWN_MODE_NORMAL);
		} catch (Exception e) {	
			e.printStackTrace();
			mgr.stop(AbstractResourceManager.SHUTDOWN_MODE_KILL);
			throw e;
		}
	}
}
