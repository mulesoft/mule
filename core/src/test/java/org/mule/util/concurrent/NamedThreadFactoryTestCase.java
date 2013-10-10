/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.concurrent;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.concurrent.TimeUnit;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SmallTest
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

    @Test
    public void testNameContextClassloader() throws InterruptedException
    {
        NamedThreadFactory threadFactory = new NamedThreadFactory(testThreadName, testClassLoader);
        Thread t = threadFactory.newThread(new Runnable()
        {

            public void run()
            {
                assertEquals(testThreadName + ".01", Thread.currentThread().getName());
                assertEquals(testClassLoader, Thread.currentThread().getContextClassLoader());
                latch.countDown();
            }
        });
        t.start();
        assertTrue(latch.await(200, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testNameIncrement() throws InterruptedException
    {
        NamedThreadFactory threadFactory = new NamedThreadFactory(testThreadName);
        Thread t = threadFactory.newThread(nullRunnable);
        assertEquals(testThreadName + ".01", t.getName());
        t = threadFactory.newThread(nullRunnable);
        assertEquals(testThreadName + ".02", t.getName());
        t = threadFactory.newThread(nullRunnable);
        assertEquals(testThreadName + ".03", t.getName());
    }

}
