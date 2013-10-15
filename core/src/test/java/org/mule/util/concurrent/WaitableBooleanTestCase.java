/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.concurrent;

import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class WaitableBooleanTestCase extends AbstractMuleTestCase
{
    protected final WaitableBoolean TRUE = new WaitableBoolean(true);
    protected final WaitableBoolean FALSE = new WaitableBoolean(false);

    @Test
    public void testCompareToBoolean()
    {
        assertEquals(0, TRUE.compareTo(true));
        assertEquals(1, TRUE.compareTo(false));
        assertEquals(0, FALSE.compareTo(false));
        assertEquals(-1, FALSE.compareTo(true));
    }

    @Test
    public void testCompareToWaitableBoolean()
    {
        assertEquals(0, TRUE.compareTo(new WaitableBoolean(true)));
        assertEquals(1, TRUE.compareTo(new WaitableBoolean(false)));
        assertEquals(0, FALSE.compareTo(new WaitableBoolean(false)));
        assertEquals(-1, FALSE.compareTo(new WaitableBoolean(true)));
        assertEquals(0, TRUE.compareTo((Object)TRUE));
    }

    @Test
    public void testCompareToObject()
    {
        assertEquals(0, TRUE.compareTo((Object)TRUE));
    }

    @Test
    public void testEquals()
    {
        assertTrue(TRUE.equals(TRUE));
        assertFalse(TRUE.equals(FALSE));
        assertFalse(FALSE.equals(TRUE));
        assertTrue(TRUE.equals(new WaitableBoolean(true)));
        assertTrue(FALSE.equals(new WaitableBoolean(false)));
        assertFalse(TRUE.equals(":-)"));
    }

    @Test
    public void testHashCode()
    {
        assertTrue(TRUE.hashCode() != FALSE.hashCode());
        assertEquals(TRUE.hashCode(), (new WaitableBoolean(true)).hashCode());
        assertEquals(FALSE.hashCode(), (new WaitableBoolean(false)).hashCode());
    }

    @Test
    public void testToString()
    {
        assertEquals("true", TRUE.toString());
        assertEquals("false", FALSE.toString());
    }

    @Test
    public void testGet()
    {
        assertTrue(TRUE.get());
        assertFalse(FALSE.get());
    }

    @Test
    public void testSet()
    {
        assertTrue(TRUE.set(true));
        assertTrue(TRUE.set(false));
        assertFalse(TRUE.set(true));
        assertFalse(FALSE.set(false));
        assertFalse(FALSE.set(true));
        assertTrue(FALSE.set(true));
    }

    @Test
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

    @Test
    public void testComplement()
    {
        assertFalse(TRUE.complement());
        assertFalse(TRUE.get());

        assertTrue(FALSE.complement());
        assertTrue(FALSE.get());
    }

    @Test
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

    @Test
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

    @Test
    public void testXor()
    {
        assertFalse((new WaitableBoolean(true)).xor(true));
        assertTrue((new WaitableBoolean(true)).xor(false));
        assertFalse((new WaitableBoolean(false)).xor(false));
        assertTrue((new WaitableBoolean(false)).xor(true));
    }

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
    public void testGetLock()
    {
        WaitableBoolean b = new WaitableBoolean(true);
        assertSame(b, b.getLock());

        b = new WaitableBoolean(true, this);
        assertSame(this, b.getLock());
    }

}
