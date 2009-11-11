/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.concurrent;

import org.mule.tck.AbstractMuleTestCase;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class NamedThreadFactoryTestCase extends AbstractMuleTestCase
{

    protected Latch latch = new Latch();
    protected String testThreadName = "myThread";
    protected ClassLoader testClassLoader = new ClassLoader()
    {
    };
    protected Runnable nullRunnable = new Runnable()
    {
        public void run()
        {
        }
    };

    public void testNameContextClassloader() throws InterruptedException
    {
        NamedThreadFactory threadFactory = new NamedThreadFactory(testThreadName, testClassLoader);
        Thread t = threadFactory.newThread(new Runnable()
        {

            public void run()
            {
                assertEquals(testThreadName + ".1", Thread.currentThread().getName());
                assertEquals(testClassLoader, Thread.currentThread().getContextClassLoader());
                latch.countDown();
            }
        });
        t.start();
        assertTrue(latch.await(200, TimeUnit.MILLISECONDS));
    }

    public void testNameIncrement() throws InterruptedException
    {
        NamedThreadFactory threadFactory = new NamedThreadFactory(testThreadName);
        Thread t = threadFactory.newThread(nullRunnable);
        assertEquals(testThreadName + ".1", t.getName());
        t = threadFactory.newThread(nullRunnable);
        assertEquals(testThreadName + ".2", t.getName());
        t = threadFactory.newThread(nullRunnable);
        assertEquals(testThreadName + ".3", t.getName());
    }

}
