/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.context.DefaultMuleContextBuilder.MULE_CONTEXT_WORKMANAGER_MAXTHREADSACTIVE;

import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.util.concurrent.Latch;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.resource.spi.work.Work;

import org.junit.Rule;
import org.junit.Test;

public class DefaultMuleWorkManagerTestCase extends AbstractMuleContextTestCase
{

    private final static int MAX_NUMBER_OF_THREADS_ALLOWED = 2;

    @Rule
    public SystemProperty systemProperty = new SystemProperty(MULE_CONTEXT_WORKMANAGER_MAXTHREADSACTIVE, String.valueOf(MAX_NUMBER_OF_THREADS_ALLOWED));

    @Test
    public void contextWorkManagerCanBeConfiguredThroughSystemProperties() throws Exception
    {
        final Thread currentThread = Thread.currentThread();
        final AtomicBoolean workExecutedInCurrentThread = new AtomicBoolean(false);
        final Latch holdThreads = new Latch();
        for (int i = 0; i < MAX_NUMBER_OF_THREADS_ALLOWED + 1; i++)
        {
            muleContext.getWorkManager().scheduleWork(new Work()
            {
                @Override
                public void release()
                {
                }

                @Override
                public void run()
                {
                    //Since current thread is used to execute work when no there are no more threads
                    // in the thread pool this should be true for the last work.
                    if (Thread.currentThread().equals(currentThread))
                    {
                        workExecutedInCurrentThread.set(true);
                        return;
                    }
                    try
                    {
                        holdThreads.await();
                    }
                    catch (InterruptedException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        assertThat(workExecutedInCurrentThread.get(), is(true));
        holdThreads.release();
    }

}
