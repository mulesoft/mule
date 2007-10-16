/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl;

import org.mule.config.ThreadingProfile;
import org.mule.impl.work.MuleWorkManager;
import org.mule.tck.AbstractMuleTestCase;

import javax.resource.spi.work.Work;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
public class MuleWorkManagerTestCase extends AbstractMuleTestCase
{
    private final transient Log logger = LogFactory.getLog(getClass());

    public void testDoWorkExecutesSynchronously() throws Exception
    {
        final Thread callerThread = Thread.currentThread();

        MuleWorkManager wm = new MuleWorkManager(ThreadingProfile.DEFAULT_THREADING_PROFILE, null);
        
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

    public void testScheduleWorkExecutesAsynchronously() throws Exception
    {
        final Thread callerThread = Thread.currentThread();

        MuleWorkManager wm = new MuleWorkManager(ThreadingProfile.DEFAULT_THREADING_PROFILE, null);

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

    public void testStartWorkExecutesAsynchronously() throws Exception
    {
        final Thread callerThread = Thread.currentThread();

        MuleWorkManager wm = new MuleWorkManager(ThreadingProfile.DEFAULT_THREADING_PROFILE, null);

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
