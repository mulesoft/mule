/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.util.concurrent;

import junit.framework.TestCase;

import org.mule.util.concurrent.WaitableBoolean;

public class WaitableBooleanTestCase extends TestCase
{
    private WaitableBoolean TRUE;
    private WaitableBoolean FALSE;

    public WaitableBooleanTestCase(String name)
    {
        super(name);
    }

    // @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        TRUE = new WaitableBoolean(true);
        FALSE = new WaitableBoolean(false);
    }

    // @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testCompareToBoolean()
    {
        assertEquals(0, TRUE.compareTo(true));
        assertEquals(1, TRUE.compareTo(false));
        assertEquals(0, FALSE.compareTo(false));
        assertEquals(-1, FALSE.compareTo(true));
    }

    public void testCompareToWaitableBoolean()
    {
        assertEquals(0, TRUE.compareTo(new WaitableBoolean(true)));
        assertEquals(1, TRUE.compareTo(new WaitableBoolean(false)));
        assertEquals(0, FALSE.compareTo(new WaitableBoolean(false)));
        assertEquals(-1, FALSE.compareTo(new WaitableBoolean(true)));
        assertEquals(0, TRUE.compareTo((Object)TRUE));
    }

    public void testCompareToObject()
    {
        assertEquals(0, TRUE.compareTo((Object)TRUE));
    }

    public void testEquals()
    {
        assertTrue(TRUE.equals(TRUE));
        assertFalse(TRUE.equals(FALSE));
        assertFalse(FALSE.equals(TRUE));
        assertTrue(TRUE.equals(new WaitableBoolean(true)));
        assertTrue(FALSE.equals(new WaitableBoolean(false)));
        assertFalse(TRUE.equals(":-)"));
    }

    public void testHashCode()
    {
        assertTrue(TRUE.hashCode() != FALSE.hashCode());
        assertEquals(TRUE.hashCode(), (new WaitableBoolean(true)).hashCode());
        assertEquals(FALSE.hashCode(), (new WaitableBoolean(false)).hashCode());
    }

    public void testToString()
    {
        assertEquals("true", TRUE.toString());
        assertEquals("false", FALSE.toString());
    }

    public void testGet()
    {
        assertTrue(TRUE.get());
        assertFalse(FALSE.get());
    }

    public void testSet()
    {
        assertTrue(TRUE.set(true));
        assertTrue(TRUE.set(false));
        assertFalse(TRUE.set(true));
        assertFalse(FALSE.set(false));
        assertFalse(FALSE.set(true));
        assertTrue(FALSE.set(true));
    }

    public void testCommit()
    {
        assertTrue(TRUE.compareAndSet(true, true));
        assertTrue(TRUE.get());
        assertFalse(TRUE.compareAndSet(false, true));
        assertTrue(TRUE.compareAndSet(true, false));
        assertFalse(TRUE.get());
        assertTrue(TRUE.compareAndSet(false, true));
        assertTrue(TRUE.get());
    }

    public void testComplement()
    {
        assertFalse(TRUE.complement());
        assertFalse(TRUE.get());

        assertTrue(FALSE.complement());
        assertTrue(FALSE.get());
    }

    public void testAnd()
    {
        assertTrue((new WaitableBoolean(true)).and(true));
        assertFalse((new WaitableBoolean(true)).and(false));
        assertFalse((new WaitableBoolean(false)).and(false));
        assertFalse((new WaitableBoolean(false)).and(true));

        assertTrue(TRUE.and(true));
        assertTrue(TRUE.get());
        assertFalse(TRUE.and(false));
        assertFalse(TRUE.get());
    }

    public void testOr()
    {
        assertTrue((new WaitableBoolean(true)).or(true));
        assertTrue((new WaitableBoolean(true)).or(false));
        assertFalse((new WaitableBoolean(false)).or(false));
        assertTrue((new WaitableBoolean(false)).or(true));

        assertTrue(TRUE.or(true));
        assertTrue(TRUE.get());
        assertTrue(TRUE.or(false));
        assertTrue(TRUE.get());
    }

    public void testXor()
    {
        assertFalse((new WaitableBoolean(true)).xor(true));
        assertTrue((new WaitableBoolean(true)).xor(false));
        assertFalse((new WaitableBoolean(false)).xor(false));
        assertTrue((new WaitableBoolean(false)).xor(true));
    }

    public void testWhenFalse() throws InterruptedException
    {
        final WaitableBoolean blocker = new WaitableBoolean(true);
        final WaitableBoolean actionPerformed = new WaitableBoolean(false);

        Runnable switcher = new Runnable()
        {
            public void run()
            {
                try
                {
                    Thread.sleep(500);
                    blocker.set(false);
                }
                catch (InterruptedException iex)
                {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(iex);
                }
            }
        };

        Runnable action = new Runnable()
        {
            public void run()
            {
                actionPerformed.set(true);
            }
        };

        new Thread(switcher).start();

        blocker.whenFalse(action);
        assertFalse(blocker.get());
        assertTrue(actionPerformed.get());
    }

    public void testWhenFalseAlreadyFalse() throws InterruptedException
    {
        final WaitableBoolean blocker = new WaitableBoolean(false);
        final WaitableBoolean actionPerformed = new WaitableBoolean(false);

        Runnable action = new Runnable()
        {
            public void run()
            {
                actionPerformed.set(true);
            }
        };

        blocker.whenFalse(action);
        assertFalse(blocker.get());
        assertTrue(actionPerformed.get());
    }

    public void testWhenTrue() throws InterruptedException
    {
        final WaitableBoolean blocker = new WaitableBoolean(false);
        final WaitableBoolean actionPerformed = new WaitableBoolean(false);

        Runnable switcher = new Runnable()
        {
            public void run()
            {
                try
                {
                    Thread.sleep(500);
                    blocker.set(true);
                }
                catch (InterruptedException iex)
                {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(iex);
                }
            }
        };

        Runnable action = new Runnable()
        {
            public void run()
            {
                actionPerformed.set(true);
            }
        };

        new Thread(switcher).start();
        blocker.whenTrue(action);
        assertTrue(blocker.get());
        assertTrue(actionPerformed.get());
    }

    public void testWhenTrueAlreadyTrue() throws InterruptedException
    {
        final WaitableBoolean blocker = new WaitableBoolean(true);
        final WaitableBoolean actionPerformed = new WaitableBoolean(false);

        Runnable action = new Runnable()
        {
            public void run()
            {
                actionPerformed.set(true);
            }
        };

        blocker.whenTrue(action);
        assertTrue(blocker.get());
        assertTrue(actionPerformed.get());
    }

    public void testGetLock()
    {
        WaitableBoolean b = new WaitableBoolean(true);
        assertSame(b, b.getLock());

        b = new WaitableBoolean(true, this);
        assertSame(this, b.getLock());
    }

}
