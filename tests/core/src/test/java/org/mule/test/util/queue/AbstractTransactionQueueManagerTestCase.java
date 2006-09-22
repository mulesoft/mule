/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.util.queue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.concurrent.Latch;
import org.mule.util.queue.Queue;
import org.mule.util.queue.QueueConfiguration;
import org.mule.util.queue.QueueSession;
import org.mule.util.queue.TransactionalQueueManager;
import org.mule.util.xa.AbstractResourceManager;

import java.util.Random;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public abstract class AbstractTransactionQueueManagerTestCase extends AbstractMuleTestCase
{

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected abstract TransactionalQueueManager createQueueManager() throws Exception;

    protected abstract boolean isPersistent();

    public void testPutTake() throws Exception
    {
        TransactionalQueueManager mgr = createQueueManager();
        mgr.start();

        QueueSession s = mgr.getQueueSession();
        Queue q = s.getQueue("queue1");

        assertEquals(0, q.size());
        // Object o = q.take();
        // assertNull(o);
        q.put("String1");
        assertEquals(1, q.size());
        Object o = q.take();
        assertNotNull(o);
        assertEquals("String1", o);
        assertEquals(0, q.size());

        mgr.stop(AbstractResourceManager.SHUTDOWN_MODE_NORMAL);
    }

    public void testTakePut() throws Exception
    {
        final TransactionalQueueManager mgr = createQueueManager();
        mgr.start();

        final Latch latch = new Latch();

        Thread t = new Thread() {
            public void run()
            {
                try {
                    latch.countDown();
                    Thread.sleep(200);
                    QueueSession s = mgr.getQueueSession();
                    Queue q = s.getQueue("queue1");
                    assertEquals(0, q.size());
                    q.put("String1");
                } catch (Exception e) {
                    // ignore, let test fail
                }
            }
        };
        t.start();
        latch.await();
        long t0 = System.currentTimeMillis();
        QueueSession s = mgr.getQueueSession();
        Queue q = s.getQueue("queue1");
        assertEquals(0, q.size());
        Object o = q.take();
        long t1 = System.currentTimeMillis();
        t.join();
        assertNotNull(o);
        assertEquals("String1", o);
        assertEquals(0, q.size());
        assertTrue(t1 - t0 > 100);

        mgr.stop(AbstractResourceManager.SHUTDOWN_MODE_NORMAL);
    }

    public void testTakePutRollbackPut() throws Exception
    {
        final TransactionalQueueManager mgr = createQueueManager();
        mgr.start();

        final Latch latch = new Latch();

        Thread t = new Thread() {
            public void run()
            {
                try {
                    latch.countDown();
                    Thread.sleep(200);
                    QueueSession s = mgr.getQueueSession();
                    Queue q = s.getQueue("queue1");
                    assertEquals(0, q.size());
                    s.begin();
                    q.put("String1");
                    s.rollback();
                    s.begin();
                    q.put("String2");
                    s.commit();
                } catch (Exception e) {
                    // ignore, let test fail
                }
            }
        };
        t.start();
        latch.await();
        long t0 = System.currentTimeMillis();
        QueueSession s = mgr.getQueueSession();
        Queue q = s.getQueue("queue1");
        assertEquals(0, q.size());
        Object o = q.take();
        long t1 = System.currentTimeMillis();
        t.join();
        assertNotNull(o);
        assertEquals("String2", o);
        assertEquals(0, q.size());
        assertTrue(t1 - t0 > 100);

        mgr.stop(AbstractResourceManager.SHUTDOWN_MODE_NORMAL);
    }

    public void testTakePutOverCapacity() throws Exception
    {
        final TransactionalQueueManager mgr = createQueueManager();
        mgr.start();
        mgr.setDefaultQueueConfiguration(new QueueConfiguration(2));

        final Latch latch = new Latch();

        Thread t = new Thread() {
            public void run()
            {
                try {
                    latch.await();
                    Thread.sleep(200);
                    QueueSession s = mgr.getQueueSession();
                    Queue q = s.getQueue("queue1");
                    Object o = q.take();
                    assertEquals("String1", o);
                } catch (Exception e) {
                    // ignore, let test fail
                }
            }
        };
        t.start();
        QueueSession s = mgr.getQueueSession();
        Queue q = s.getQueue("queue1");
        assertEquals(0, q.size());
        q.put("String1");
        q.put("String2");
        latch.countDown();
        long t0 = System.currentTimeMillis();
        q.put("String3");
        long t1 = System.currentTimeMillis();
        t.join();
        assertEquals(2, q.size());
        assertTrue(t1 - t0 > 100);

        mgr.stop(AbstractResourceManager.SHUTDOWN_MODE_NORMAL);
    }

    public void testPutWithPersistence() throws Exception
    {
        if (isPersistent()) {
            TransactionalQueueManager mgr = createQueueManager();

            try {
                mgr.start();

                QueueSession s = mgr.getQueueSession();
                Queue q = s.getQueue("queue1");
                q.put("String1");
                assertEquals(1, q.size());

                q = s.getQueue("queue1");
                assertEquals(1, q.size());
            } finally {
                mgr.stop(AbstractResourceManager.SHUTDOWN_MODE_NORMAL);
            }

            mgr = createQueueManager();
            try {
                mgr.start();
                QueueSession s = mgr.getQueueSession();
                Queue q = s.getQueue("queue1");
                assertEquals(1, q.size());
            } finally {
                mgr.stop(AbstractResourceManager.SHUTDOWN_MODE_NORMAL);
            }
        } else {
            logger.info("Ignoring test because queue manager is not persistent");
        }
    }

    public void testTransactedPutCommitWithPersistence() throws Exception
    {
        if (isPersistent()) {
            TransactionalQueueManager mgr = createQueueManager();

            try {
                mgr.start();

                QueueSession s = mgr.getQueueSession();
                Queue q = s.getQueue("queue1");
                s.begin();
                q.put("String1");
                assertEquals(1, q.size());
                s.commit();
                assertEquals(1, q.size());

                s = mgr.getQueueSession();
                q = s.getQueue("queue1");
                assertEquals(1, q.size());

                mgr.stop(AbstractResourceManager.SHUTDOWN_MODE_NORMAL);

                mgr = createQueueManager();
                mgr.start();
                s = mgr.getQueueSession();
                q = s.getQueue("queue1");
                assertEquals(1, q.size());
            } finally {
                mgr.stop(AbstractResourceManager.SHUTDOWN_MODE_NORMAL);
            }
        } else {
            logger.info("Ignoring test because queue manager is not persistent");
        }
    }

    public void testTransactedPutRollbackWithPersistence() throws Exception
    {
        if (isPersistent()) {
            TransactionalQueueManager mgr = createQueueManager();

            try {
                mgr.start();

                QueueSession s = mgr.getQueueSession();
                Queue q = s.getQueue("queue1");
                s.begin();
                q.put("String1");
                assertEquals(1, q.size());
                s.rollback();
                assertEquals(0, q.size());

                s = mgr.getQueueSession();
                q = s.getQueue("queue1");
                assertEquals(0, q.size());

                mgr.stop(AbstractResourceManager.SHUTDOWN_MODE_NORMAL);

                mgr = createQueueManager();
                mgr.start();
                s = mgr.getQueueSession();
                q = s.getQueue("queue1");
                assertEquals(0, q.size());

            } finally {

                mgr.stop(AbstractResourceManager.SHUTDOWN_MODE_NORMAL);

            }
        } else {
            logger.info("Ignoring test because queue manager is not persistent");
        }
    }

    public void testTransactionsOnMultipleQueues() throws Exception
    {

        TransactionalQueueManager mgr = createQueueManager();

        try {
            mgr.start();

            QueueSession s1 = mgr.getQueueSession();
            QueueSession s2 = mgr.getQueueSession();

            Queue q1s1 = s1.getQueue("queue1");
            Queue q1s2 = s2.getQueue("queue1");
            Queue q2s1 = s1.getQueue("queue2");
            Queue q2s2 = s2.getQueue("queue2");

            q1s1.put("String1");
            assertEquals(1, q1s1.size());
            assertEquals(1, q1s2.size());

            s1.begin();

            Object o = q1s1.take();
            assertNotNull(o);
            assertEquals("String1", o);
            assertEquals(0, q1s1.size());
            assertEquals(0, q1s2.size());
            q2s1.put("String2");
            assertEquals(1, q2s1.size());
            assertEquals(0, q2s2.size());

            s1.commit();

            assertEquals(0, q1s1.size());
            assertEquals(0, q1s2.size());
            assertEquals(1, q2s1.size());
            assertEquals(1, q2s2.size());

            s1.begin();

            o = q2s1.take();
            assertNotNull(o);
            assertEquals("String2", o);
            assertEquals(0, q1s1.size());
            assertEquals(0, q1s2.size());
            assertEquals(0, q2s1.size());
            assertEquals(0, q2s2.size());

            q1s1.put("String1");

            assertEquals(1, q1s1.size());
            assertEquals(0, q1s2.size());
            assertEquals(0, q2s1.size());
            assertEquals(0, q2s2.size());

            s1.rollback();

            assertEquals(0, q1s1.size());
            assertEquals(0, q1s2.size());
            assertEquals(1, q2s1.size());
            assertEquals(1, q2s2.size());

        } finally {
            mgr.stop(AbstractResourceManager.SHUTDOWN_MODE_NORMAL);
        }
    }

    public void testPoll() throws Exception
    {

        final TransactionalQueueManager mgr = createQueueManager();

        try {
            mgr.start();

            QueueSession s = mgr.getQueueSession();
            Queue q = s.getQueue("queue1");

            assertEquals(q.size(), 0);
            Object o = q.poll(0);
            assertEquals(q.size(), 0);
            assertNull(o);
            o = q.poll(1000);
            assertEquals(q.size(), 0);
            assertNull(o);
            q.put("String1");
            assertEquals(q.size(), 1);
            o = q.poll(0);
            assertEquals(q.size(), 0);
            assertEquals("String1", o);

            new Thread(new Runnable() {
                public void run()
                {
                    try {
                        Thread.sleep(500);
                        QueueSession s = mgr.getQueueSession();
                        Queue q = s.getQueue("queue1");
                        q.put("String1");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            o = q.poll(1000);
            assertEquals(q.size(), 0);
            assertEquals("String1", o);

        } finally {
            mgr.stop(AbstractResourceManager.SHUTDOWN_MODE_NORMAL);
        }
    }

    public void testPeek() throws Exception
    {

        TransactionalQueueManager mgr = createQueueManager();

        try {
            mgr.start();

            QueueSession s = mgr.getQueueSession();
            Queue q = s.getQueue("queue1");

            assertEquals(q.size(), 0);
            Object o = q.peek();
            assertEquals(q.size(), 0);
            assertNull(o);
            q.put("String1");
            assertEquals(q.size(), 1);
            o = q.peek();
            assertEquals(q.size(), 1);
            assertEquals("String1", o);

        } finally {
            mgr.stop(AbstractResourceManager.SHUTDOWN_MODE_NORMAL);
        }
    }

    public void testOffer() throws Exception
    {

        final TransactionalQueueManager mgr = createQueueManager();
        mgr.setDefaultQueueConfiguration(new QueueConfiguration(1));
        try {
            mgr.start();

            QueueSession s = mgr.getQueueSession();
            Queue q = s.getQueue("queue1");

            assertEquals(q.size(), 0);
            assertTrue(q.offer("String1", 0L));
            assertEquals(q.size(), 1);
            assertFalse(q.offer("String2", 1000));
            assertEquals(q.size(), 1);

            new Thread(new Runnable() {
                public void run()
                {
                    try {
                        Thread.sleep(500);
                        QueueSession s = mgr.getQueueSession();
                        Queue q = s.getQueue("queue1");
                        assertEquals("String1", q.take());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            assertTrue(q.offer("String2", 1000));
            assertEquals(q.size(), 1);
        } finally {
            mgr.stop(AbstractResourceManager.SHUTDOWN_MODE_NORMAL);
        }
    }

    public void testBench() throws Exception
    {

        TransactionalQueueManager mgr = createQueueManager();

        try {
            mgr.start();

            QueueSession s = mgr.getQueueSession();
            Queue q = s.getQueue("queue1");

            Random rnd = new Random();
            long t0 = System.currentTimeMillis();
            for (int i = 0; i < 1; i++) {
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

        } finally {
            mgr.stop(AbstractResourceManager.SHUTDOWN_MODE_NORMAL);
        }
    }

}
