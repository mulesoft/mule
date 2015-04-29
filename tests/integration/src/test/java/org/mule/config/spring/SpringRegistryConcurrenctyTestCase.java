/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Startable;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class SpringRegistryConcurrenctyTestCase extends FunctionalTestCase
{

    private static final int THREAD_COUNT = 10;
    private static final long LATCH_TIMEOUT = 5;
    private static final TimeUnit LATCH_TIME_UNIT = TimeUnit.SECONDS;

    private CountDownLatch latch;
    private List<Thread> threads;
    private List<Map<String, Object>> reads;

    @Override
    protected String[] getConfigFiles()
    {
        return new String[] {};
    }

    @Override
    protected void doSetUp() throws Exception
    {
        latch = new CountDownLatch(THREAD_COUNT);
        threads = new ArrayList<>(THREAD_COUNT);
        reads = new ArrayList<>(THREAD_COUNT);
    }

    @Test
    public void concurrentReads() throws Exception
    {
        final int registrySize = muleContext.getRegistry().lookupByType(Object.class).values().size();

        for (int i = 0; i < THREAD_COUNT; i++)
        {
            threads.add(new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    synchronized (reads)
                    {
                        try
                        {
                            reads.add(muleContext.getRegistry().lookupByType(Object.class));
                        }
                        finally
                        {
                            latch.countDown();
                        }
                    }
                }
            }));
        }

        startAllThreads();
        waitOnLatch();

        assertThat(reads, hasSize(THREAD_COUNT));
        for (Map<String, Object> objects : reads)
        {
            assertThat(objects.values(), hasSize(registrySize));
        }
    }

    @Test
    public void concurrentReadsAndWrites() throws Exception
    {
        final CountDownLatch auxiliaryLatch = new CountDownLatch(THREAD_COUNT);
        initialiseReadAndWriteThreads(auxiliaryLatch);

        Object object = new Startable()
        {
            @Override
            public void start() throws MuleException
            {
                try
                {
                    startAllThreads();
                    waitOnLatch();
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        };

        final String key = "KEY";
        muleContext.getRegistry().registerObject(key, object);

        waitOnLatch();
        waitOnLatch(auxiliaryLatch);

        assertThat(reads, hasSize(THREAD_COUNT));
        for (Map<String, Object> objects : reads)
        {
            assertThat(objects.containsKey(key), is(true));
        }
    }

    @Test
    public void concurrentReadsAnRemove() throws Exception
    {
        final String key = "KEY";

        Object object = new Disposable()
        {
            @Override
            public void dispose()
            {
                try
                {
                    startAllThreads();
                    waitOnLatch();
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        };

        muleContext.getRegistry().registerObject(key, object);

        final CountDownLatch auxiliaryLatch = new CountDownLatch(THREAD_COUNT);
        initialiseReadAndWriteThreads(auxiliaryLatch);

        muleContext.getRegistry().unregisterObject(key);

        waitOnLatch();
        waitOnLatch(auxiliaryLatch);

        assertThat(reads, hasSize(THREAD_COUNT));
        for (Map<String, Object> objects : reads)
        {
            assertThat(objects.containsKey(key), is(false));
        }
    }


    private void initialiseReadAndWriteThreads(final CountDownLatch auxiliaryLatch)
    {
        for (int i = 0; i < THREAD_COUNT; i++)
        {
            threads.add(new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    latch.countDown();
                    synchronized (reads)
                    {
                        reads.add(muleContext.getRegistry().lookupByType(Object.class));
                    }
                    auxiliaryLatch.countDown();
                }
            }));
        }
    }

    private void startAllThreads()
    {
        for (Thread thread : threads)
        {
            thread.start();
        }
    }

    private void waitOnLatch() throws Exception
    {
        waitOnLatch(latch);
    }

    private void waitOnLatch(CountDownLatch latch) throws Exception
    {
        assertThat(latch.await(LATCH_TIMEOUT, LATCH_TIME_UNIT), is(true));
    }
}
