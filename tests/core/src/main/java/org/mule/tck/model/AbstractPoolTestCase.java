/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.model;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.mule.impl.MuleDescriptor;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.umo.UMODescriptor;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.model.UMOPoolFactory;
import org.mule.util.ObjectPool;

/**
 * <code>AbstractPoolTestCase</code> TODO (document class)
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractPoolTestCase extends AbstractMuleTestCase
{
    public static final byte FAIL_WHEN_EXHAUSTED = 0;
    public static final byte GROW_WHEN_EXHAUSTED = 1;
    public static final byte BLOCK_WHEN_EXHAUSTED = 2;

    public static final byte DEFAULT_POOL_SIZE = 3;
    public static final long DEFAULT_WAIT = 1500;

    public AbstractPoolTestCase()
    {
        super();

    }

    protected void doSetUp() throws Exception
    {
        // Initialise the manager
        getManager(true);
    }

    public void testCreatePool() throws Exception
    {

        MuleDescriptor d = getTestDescriptor("orange", Orange.class.getName());
        ObjectPool pool = createPool(d, FAIL_WHEN_EXHAUSTED);

        assertNotNull(pool);
        assertEquals(0, pool.getSize());

        Object borrowed = pool.borrowObject();
        assertNotNull(borrowed);
        assertEquals(1, pool.getSize());
        pool.returnObject(borrowed);

        borrowed = pool.borrowObject();
        assertNotNull(borrowed);
        assertEquals(1, pool.getSize());
        Object borrowed2 = pool.borrowObject();
        assertNotNull(borrowed2);
        assertEquals(2, pool.getSize());
    }

    public void testFailOnExhaust() throws Exception
    {

        ObjectPool pool = createPool(getTestDescriptor("orange", Orange.class.getName()),
                FAIL_WHEN_EXHAUSTED);
        Object borrowed = null;

        for (int i = 0; i < pool.getMaxSize(); i++) {
            borrowed = pool.borrowObject();
            assertNotNull(borrowed);
            assertEquals(pool.getSize(), i + 1);
        }

        try {
            borrowed = pool.borrowObject();
            fail("Should throw an Exception");
        }
        catch (Exception e) {
            // expected
        }
    }

    public void testBlockExpiryOnExhaust() throws Exception
    {
        ObjectPool pool = createPool(getTestDescriptor("orange", Orange.class.getName()),
                BLOCK_WHEN_EXHAUSTED);
        Object borrowed = null;

        assertEquals(0, pool.getSize());
        borrowed = pool.borrowObject();
        assertNotNull(borrowed);
        borrowed = pool.borrowObject();
        assertNotNull(borrowed);
        borrowed = pool.borrowObject();
        assertNotNull(borrowed);
        assertEquals(3, pool.getSize());

        // TODO
        // long starttime = System.currentTimeMillis();
        try {
            borrowed = pool.borrowObject();
            fail("Should throw an Exception");
        }
        catch (Exception e) {
            // TODO
            // long totalTime = System.currentTimeMillis() - starttime;
            // Need to allow for alittle variance in system time
            // This is unreliable
            // assertTrue(totalTime < (DEFAULT_WAIT + 300) && totalTime >
            // (DEFAULT_WAIT - 300));
        }
    }

    public void testBlockOnExhaust() throws Exception
    {
        ObjectPool pool = createPool(getTestDescriptor("orange", Orange.class.getName()),
                BLOCK_WHEN_EXHAUSTED);
        Object borrowed = null;

        assertEquals(0, pool.getSize());

        borrowed = pool.borrowObject();
        borrowed = pool.borrowObject();
        assertEquals(2, pool.getSize());

        // TODO
        // long starttime = System.currentTimeMillis();
        long borrowerWait = 500;
        Borrower borrower = new Borrower(pool, borrowerWait);
        borrower.start();
        // Make sure the borrower borrows first
        try {
            Thread.sleep(200);
        }
        catch (InterruptedException e) {
            // ignore
        }

        borrowed = pool.borrowObject();
        // TODO
        // long totalTime = System.currentTimeMillis() - starttime;
        // Need to allow for alittle variance in system time
        // This is unreliable
        // assertTrue(totalTime < (borrowerWait + 300) && totalTime >
        // (borrowerWait -300));

        assertNotNull(borrowed);
    }

    public void testGrowOnExhaust() throws Exception
    {
        ObjectPool pool = createPool(getTestDescriptor("orange", Orange.class.getName()),
                GROW_WHEN_EXHAUSTED);

        Object borrowed = pool.borrowObject();
        borrowed = pool.borrowObject();
        borrowed = pool.borrowObject();
        assertEquals(3, pool.getSize());
        assertEquals(3, pool.getMaxSize());

        // Should now grow
        borrowed = pool.borrowObject();
        assertNotNull(borrowed);

        assertEquals(4, pool.getSize());
    }

    public void testClearPool() throws Exception
    {

        ObjectPool pool = createPool(getTestDescriptor("orange", Orange.class.getName()),
                FAIL_WHEN_EXHAUSTED);

        Object borrowed = pool.borrowObject();
        assertEquals(1, pool.getSize());
        pool.returnObject(borrowed);

        pool.clearPool();
        assertEquals(0, pool.getSize());

        borrowed = pool.borrowObject();
        assertEquals(1, pool.getSize());
    }

    public void testCreateFromFactory() throws Exception
    {
        UMODescriptor descriptor = getTestDescriptor("orange", Orange.class.getName());
        UMOPoolFactory factory = getPoolFactory();
        ObjectPool pool = factory.createPool(descriptor);
        assertNotNull(pool);
    }

    public void testPoolLifecycle() throws Exception
    {
        MuleDescriptor d = getTestDescriptor("orange", Orange.class.getName());
        ObjectPool pool = createPool(d, FAIL_WHEN_EXHAUSTED);

        assertNotNull(pool);
        assertEquals(0, pool.getSize());

    }

    private class Borrower extends Thread
    {
        private ObjectPool pool;
        private long time;

        public Borrower(ObjectPool pool, long time)
        {
            super("Borrower");
            if (pool == null) {
                throw new IllegalArgumentException("Pool cannot be null");
            }
            this.pool = pool;
            if (time < 500) {
                time = 500;
            }
            this.time = time;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Runnable#run()
         */
        public void run()
        {
            try {
                Object object = pool.borrowObject();
                try {
                    sleep(time);
                }
                catch (InterruptedException e) {
                    // ignore
                }
                pool.returnObject(object);
            }
            catch (Exception e) {
                fail("Borrower thread failed:\n" + ExceptionUtils.getStackTrace(e));
            }
        }

    }

    public abstract ObjectPool createPool(MuleDescriptor descriptor, byte action)
            throws InitialisationException;

    public abstract UMOPoolFactory getPoolFactory();
}
