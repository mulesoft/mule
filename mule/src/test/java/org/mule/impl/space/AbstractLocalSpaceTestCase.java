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
 */
package org.mule.impl.space;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.space.UMOSpace;
import org.mule.umo.space.UMOSpaceException;
import org.mule.util.concurrent.Latch;
import org.mule.util.queue.TransactionalQueueManager;
import org.mule.util.xa.AbstractResourceManager;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractLocalSpaceTestCase extends AbstractMuleTestCase {

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected abstract DefaultSpaceFactory getSpaceFactory() throws Exception;

    protected abstract boolean isPersistent();

    public void testPutTake() throws Exception {
        final UMOSpace space = getSpaceFactory().create("test");
        try {
            assertNull(space.takeNoWait());
            space.put("String1");
            Object o = space.take(1000);
            assertNotNull(o);
            assertEquals("String1", o);
            assertNull(space.takeNoWait());
        } finally {
            space.dispose();
        }
    }

    public void testTakePut() throws Exception {

        final UMOSpace space = getSpaceFactory().create("test");
        try {
            final Latch latch = new Latch();

            Thread t = new Thread() {
                public void run() {
                    try {
                        latch.countDown();
                        Thread.sleep(200);

                        assertNull(space.takeNoWait());
                        space.put("String1");
                    } catch (Exception e) {
                        logger.warn(e);
                    }
                }
            };
            t.start();
            latch.await();
            long t0 = System.currentTimeMillis();
            assertNull(space.takeNoWait());
            Object o = space.take();
            long t1 = System.currentTimeMillis();
            t.join();
            assertNotNull(o);
            assertEquals("String1", o);
            assertNull(space.takeNoWait());
            assertTrue(t1 - t0 > 100);
        } finally {
            space.dispose();
        }
    }

    public void testTakePutRollbackPut() throws Exception {
        final Latch latch = new Latch();

        final UMOSpace space = getSpaceFactory().create("test");

        try {
            Thread t = new Thread() {
                public void run() {
                    try {
                        latch.countDown();
                        Thread.sleep(200);
                        assertNull(space.takeNoWait());
                        space.beginTransaction();
                        space.put("String1");
                        space.rollbackTransaction();
                        space.beginTransaction();
                        space.put("String2");
                        space.commitTransaction();
                    } catch (Exception e) {
                        logger.warn(e);
                    }
                }
            };
            t.start();
            latch.await();
            long t0 = System.currentTimeMillis();
            assertNull(space.takeNoWait());
            Object o = space.take();
            long t1 = System.currentTimeMillis();
            t.join();
            assertNotNull(o);
            assertEquals("String2", o);
            assertNull(space.takeNoWait());
            assertTrue(t1 - t0 > 100);
        } finally {
            space.dispose();
        }
    }

    public void testTakePutOverCapacity() throws Exception {
        DefaultSpaceFactory factory = getSpaceFactory();
        factory.setCapacity(2);
        final UMOSpace space = factory.create("test");

        try {
            final Latch latch = new Latch();

            Thread t = new Thread() {
                public void run() {
                    try {
                        latch.await();
                        Thread.sleep(200);
                        Object o = space.take();
                        assertEquals("String1", o);
                    } catch (Exception e) {
                        logger.warn(e);
                    }
                }
            };
            t.start();
            assertNull(space.takeNoWait());
            space.put("String1");
            space.put("String2");
            latch.countDown();
            long t0 = System.currentTimeMillis();
            space.put("String3");
            long t1 = System.currentTimeMillis();
            t.join();
            assertNotNull(space.takeNoWait());
            assertNotNull(space.takeNoWait());
            assertNull(space.takeNoWait());
            assertTrue(t1 - t0 > 100);
        } finally {
            space.dispose();
        }
    }

    public void testPutWithPersistence() throws Exception {
        if (isPersistent()) {
            UMOSpace space = getSpaceFactory().create("test");

            try {
                space.put("String1x");
            } catch (UMOSpaceException ex) {
                logger.warn(ex);
            } finally {
                space.dispose();
            }

            space = getSpaceFactory().create("test");
            try {
                assertEquals("String1x", space.take(1000));
            } catch (UMOSpaceException ex) {
                logger.warn(ex);
            } finally {
                space.dispose();
            }
        } else {
            logger.info("Ignoring test because queue manager is not persistent");
        }
    }

    public void testTransactedPutCommitWithPersistence() throws Exception {
        if (isPersistent()) {
            UMOSpace space = getSpaceFactory().create("test");
            try {
                space.beginTransaction();
                space.put("String1");
                assertEquals(1, space.size());
                space.commitTransaction();
                assertEquals(1, space.size());

                space.dispose();

                space = getSpaceFactory().create("test");
                assertEquals(1, space.size());
                assertNotNull(space.takeNoWait());
                assertNull(space.takeNoWait());
            } finally {
                space.dispose();
            }
        } else {
            logger.info("Ignoring test because queue manager is not persistent");
        }
    }

    public void testTransactedPutRollbackWithPersistence() throws Exception {
        if (isPersistent()) {
            UMOSpace space = getSpaceFactory().create("test");

            try {
                space.beginTransaction();
                space.put("String1");
                assertEquals(1, space.size());
                space.rollbackTransaction();
                assertEquals(0, space.size());

                space.dispose();

                space = getSpaceFactory().create("test");
                assertEquals(0, space.size());

            } finally {

                space.dispose();

            }
        } else {
            logger.info("Ignoring test because space is not persistent");
        }
    }

    public void testTransactionsOnChildSpaces() throws Exception {

        TransactionalQueueManager manager = new TransactionalQueueManager();

        DefaultSpaceFactory factory = getSpaceFactory();
        manager.setPersistenceStrategy(factory.getPersistenceStrategy());
        factory.setQueueManager(manager);
        factory.setPersistenceStrategy(null);

        manager.start();
        DefaultSpace space1a = (DefaultSpace) factory.create("space1");
        DefaultSpace space1b = space1a.createChild("child");

        DefaultSpace space2a = (DefaultSpace)factory.create("space2");
        DefaultSpace space2b = space2a.createChild("child");

//        assertEquals("space1.child", space1b.getName());
//        assertEquals("space2.child", space2b.getName());

//        mgr.start();
//
//            QueueSession s1 = mgr.getQueueSession();
//            Queue q1s1 = s1.getQueue("queue1");
//            Queue q2s1 = s1.getQueue("queue2");

//            QueueSession s2 = mgr.getQueueSession();
//            Queue q1s2 = s2.getQueue("queue1");
//            Queue q2s2 = s2.getQueue("queue2");

        try {

           space1a.put("String1");
            assertEquals(1, space1a.size());
            assertEquals(1, space1b.size());

            space1a.beginTransaction();

            Object o = space1a.take();
            assertNotNull(o);
            assertEquals("String1", o);
            assertEquals(0, space1a.size());
            assertEquals(0, space1b.size());
            space2a.put("String2");
            assertEquals(1, space2a.size());
            assertEquals(1, space2b.size());

            space1a.commitTransaction();

            assertEquals(0, space1a.size());
            assertEquals(0, space1b.size());
            assertEquals(1, space2a.size());
            assertEquals(1, space2b.size());

            space1a.beginTransaction();

            o = space2a.take();
            assertNotNull(o);
            assertEquals("String2", o);
            assertEquals(0, space1a.size());
            assertEquals(0, space1b.size());
            assertEquals(0, space2a.size());
            assertEquals(0, space2b.size());

            space1a.put("String1");
            space2a.put("String1");

            assertEquals(1, space1a.size());
            assertEquals(1, space1b.size());
            assertEquals(1, space2a.size());
            assertEquals(1, space2b.size());

            space1a.rollbackTransaction();

            assertEquals(0, space1a.size());
            assertEquals(0, space1b.size());
            assertEquals(1, space2a.size());
            assertEquals(1, space2b.size());

        } finally {
            space1a.dispose();
            space2a.dispose();
            manager.stop(AbstractResourceManager.SHUTDOWN_MODE_NORMAL);
        }
    }

    public void testPoll() throws Exception {
        final UMOSpace space = getSpaceFactory().create("test");
        try {
            assertEquals(space.size(), 0);
            Object o = space.take(0);
            assertEquals(space.size(), 0);
            assertNull(o);
            o = space.take(1000);
            assertEquals(space.size(), 0);
            assertNull(o);
            space.put("String1");
            assertEquals(space.size(), 1);
            o = space.take(0);
            assertEquals(space.size(), 0);
            assertEquals("String1", o);

            new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(500);
                        space.put("String1");
                    } catch (Exception e) {
                        logger.warn(e);
                    }
                }
            }).start();
            o = space.take(1000);
            assertEquals(space.size(), 0);
            assertEquals("String1", o);

        } finally {
            space.dispose();
        }
    }

    public void testBenchmarkSpace() throws Exception {


        final UMOSpace space = getSpaceFactory().create("test");

        try {
            //default capacity is 1024
            long t0 = System.currentTimeMillis();
            for (int x = 0; x < 1; x++) {
                for (int i = 0; i < 1000; i++) {
                    space.put(new Integer(i*x));
                }
                while (space.size() > 0) {
                    space.take();
                }
            }

            long t1 = System.currentTimeMillis();

            logger.info("Time to process 1000 hits: " + (t1 - t0) + " ms");

        } finally {
            space.dispose();
        }
    }

}
