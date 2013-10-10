/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule;

import org.mule.api.config.ThreadingProfile;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.work.MuleWorkManager;

import javax.resource.spi.work.Work;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Tests the following behavior:
 * <ol>
 *  <li>ScheduleWorkExecutor - e.g. use the backing thread pool to execute work items asynchronously
 *  <li>StartWorkExecutor - block till the work is started, then async
 *  <li>SyncWorkExecutor - blocking executor, meaning we should be running in the very same thread.
 *</ol>
 * It's not really important to make a distinction between <code>scheduleWork()</code> and
 * <code>startWork()</code> for this test, thus they just check for async execution.
 */
public class MuleWorkManagerTestCase extends AbstractMuleContextTestCase
{
    private final transient Log logger = LogFactory.getLog(getClass());

    @Test
    public void testDoWorkExecutesSynchronously() throws Exception
    {
        final Thread callerThread = Thread.currentThread();

        MuleWorkManager wm = new MuleWorkManager(ThreadingProfile.DEFAULT_THREADING_PROFILE, null, 5000);
        wm.setMuleContext(muleContext);
        
        try
        {
            wm.start();
            
            wm.doWork(new Work()
            {
                public void release()
                {
                    // no-op
                }

                public void run()
                {
                    Thread calleeThread = Thread.currentThread();
                    assertEquals("WorkManager.doWork() should have been executed in the same thread.",
                                 callerThread, calleeThread);
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("WORK: " + Thread.currentThread());
                    }
                }
            });
            if (logger.isDebugEnabled())
            {
                logger.debug("MAIN: " + Thread.currentThread());
            }
        }
        finally
        {
            wm.dispose();
        }

    }

    @Test
    public void testScheduleWorkExecutesAsynchronously() throws Exception
    {
        final Thread callerThread = Thread.currentThread();

        MuleWorkManager wm = new MuleWorkManager(ThreadingProfile.DEFAULT_THREADING_PROFILE, null, 5000);
        wm.setMuleContext(muleContext);

        try
        {
            wm.start();

            wm.scheduleWork(new Work()
            {
                public void release()
                {
                    // no-op
                }

                public void run()
                {
                    Thread calleeThread = Thread.currentThread();
                    assertFalse("WorkManager.scheduleWork() should have been executed in a different thread.",
                                callerThread.equals(calleeThread));
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("WORK: " + Thread.currentThread());
                    }
                }
            });
            if (logger.isDebugEnabled())
            {
                logger.debug("MAIN: " + Thread.currentThread());
            }
        }
        finally
        {
            wm.dispose();
        }

    }

    @Test
    public void testStartWorkExecutesAsynchronously() throws Exception
    {
        final Thread callerThread = Thread.currentThread();

        MuleWorkManager wm = new MuleWorkManager(ThreadingProfile.DEFAULT_THREADING_PROFILE, null, 5000);
        wm.setMuleContext(muleContext);

        try
        {
            wm.start();

            wm.startWork(new Work()
            {
                public void release()
                {
                    // no-op
                }

                public void run()
                {
                    Thread calleeThread = Thread.currentThread();
                    assertFalse("WorkManager.startWork() should have been executed in a different thread.",
                                callerThread.equals(calleeThread));
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("WORK: " + Thread.currentThread());
                    }
                }
            });
            if (logger.isDebugEnabled())
            {
                logger.debug("MAIN: " + Thread.currentThread());
            }
        }
        finally
        {
            wm.dispose();
        }

    }

}
