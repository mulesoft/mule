/* 
 * $Id$
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

package org.mule.test.util.concurrent;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;

import org.mule.util.concurrent.Latch;

/**
 * @author Holger Hoffstaette
 */

public class CountDownLatchTestCase extends TestCase
{

    public CountDownLatchTestCase(String name)
    {
        super(name);
    }

    public void testLockCannotBeInterrupted() throws InterruptedException
    {
        final Latch toWaitFor = new Latch();

        final Latch taskIsRunning = new Latch();
        final Latch taskHasFinished = new Latch();
        final AtomicBoolean taskWasInterrupted = new AtomicBoolean(false);

        Runnable waiting = new Runnable()
        {
            public void run()
            {
                taskIsRunning.countDown();

                // Lock.lock() does not declare InterruptedException
                toWaitFor.lock();

                // pass interrupted status to outside world
                if (Thread.currentThread().isInterrupted())
                {
                    taskWasInterrupted.set(true);
                }

                taskHasFinished.countDown();
            }
        };

        // start latch observer
        Thread waitingThread = new Thread(waiting);
        waitingThread.start();

        // wait for observer to start
        taskIsRunning.await();
        Thread.sleep(500);

        // kick thread multiple times; the sleep intervals make sure that the
        // interrupted thread gets a chance to do something
        waitingThread.interrupt();
        Thread.sleep(100);
        waitingThread.interrupt();
        Thread.sleep(100);
        waitingThread.interrupt();

        // give thread a chance to die
        Thread.sleep(500);

        // make sure lock() has NOT yet returned
        assertFalse(taskWasInterrupted.get());
        assertEquals(1, taskHasFinished.getCount());

        // now pull the official plug & see what happens
        toWaitFor.countDown();
        taskHasFinished.await();
        assertTrue(taskWasInterrupted.get());
        assertEquals(0, toWaitFor.getCount());
    }

}
