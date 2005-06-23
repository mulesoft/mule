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
package org.mule.test.util.queue;

import java.util.Random;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.util.queue.FilePersistenceStrategy;
import org.mule.util.queue.JournalPersistenceStrategy;
import org.mule.util.queue.Queue;
import org.mule.util.queue.QueueSession;
import org.mule.util.queue.TransactionalQueueManager;
import org.mule.util.xa.AbstractResourceManager;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class BenchmarkTestCase extends TestCase
{

    private static final Log logger = LogFactory.getLog(BenchmarkTestCase.class);

    // private static final int WORKERS = 10;
    // private static final int OUTERLOOP = 100;
    // private static final int INNERLOOP = 500;
    private static final int WORKERS = 1;
    private static final int OUTERLOOP = 1;
    private static final int INNERLOOP = 1;
    private static final int OBJSIZE = 256;

    protected TransactionalQueueManager createFileQueueManager() throws Exception
    {
        TransactionalQueueManager mgr = new TransactionalQueueManager();
        mgr.setPersistenceStrategy(new FilePersistenceStrategy());
        return mgr;
    }

    protected TransactionalQueueManager createJournalQueueManager() throws Exception
    {
        TransactionalQueueManager mgr = new TransactionalQueueManager();
        mgr.setPersistenceStrategy(new JournalPersistenceStrategy());
        return mgr;
    }

    public void testBench() throws Exception
    {
        benchmark(createJournalQueueManager());
        benchmark(createFileQueueManager());
    }

    public static void main(String[] args) throws Exception
    {
        new BenchmarkTestCase().testBench();
    }

    protected static class Worker extends Thread
    {
        private TransactionalQueueManager mgr;
        private String queue;

        public Worker(TransactionalQueueManager mgr, String queue)
        {
            this.mgr = mgr;
            this.queue = queue;
        }

        public void run()
        {
            Random rnd = new Random();
            try {
                QueueSession s = mgr.getQueueSession();
                Queue q = s.getQueue(queue);

                for (int i = 0; i < OUTERLOOP; i++) {
                    for (int j = 0; j < INNERLOOP; j++) {
                        byte[] o = new byte[(rnd.nextInt(16) + 1) * OBJSIZE];
                        q.put(o);
                    }
                    while (q.size() > 0) {
                        q.take();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    protected void benchmark(TransactionalQueueManager mgr) throws Exception
    {
        logger.info("================================");
        logger.info("Running test: " + mgr.getPersistenceStrategy().getClass().getName());
        logger.info("================================");

        try {
            mgr.start();

            long t0 = System.currentTimeMillis();
            Worker[] w = new Worker[WORKERS];
            for (int i = 0; i < w.length; i++) {
                w[i] = new Worker(mgr, "queue" + i);
                w[i].setDaemon(true);
                w[i].start();
            }
            for (int i = 0; i < w.length; i++) {
                w[i].join();
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
