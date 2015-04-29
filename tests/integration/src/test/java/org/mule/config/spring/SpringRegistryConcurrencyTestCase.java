/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Startable;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.concurrent.Latch;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class SpringRegistryConcurrencyTestCase extends FunctionalTestCase
{

    private static final String KEY = "KEY";
    private static final long LATCH_TIMEOUT = 5;

    private Latch latch;
    private Latch auxiliaryLatch;
    private Map<String, Object> lookupObjects;

    @Override
    protected String[] getConfigFiles()
    {
        return new String[] {};
    }

    @Override
    protected void doSetUp() throws Exception
    {
        latch = new Latch();
        auxiliaryLatch = new Latch();
        lookupObjects = null;
    }

    @Test
    public void concurrentReadAndWrite() throws Exception
    {
        final Thread thread = newReaderThread();

        Object object = new Startable()
        {
            @Override
            public void start() throws MuleException
            {
                try
                {
                    thread.start();
                    waitOn(latch);
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        };


        muleContext.getRegistry().registerObject(KEY, object);

        waitOn(auxiliaryLatch);
        assertThat(lookupObjects.containsKey(KEY), is(true));
    }


    @Test
    public void concurrentReadAnRemove() throws Exception
    {
        final Thread thread = newReaderThread();

        Object object = new Disposable()
        {
            @Override
            public void dispose()
            {
                try
                {
                    thread.start();
                    waitOn(latch);
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        };

        muleContext.getRegistry().registerObject(KEY, object);
        muleContext.getRegistry().unregisterObject(KEY);
        waitOn(auxiliaryLatch);

        assertThat(lookupObjects.containsKey(KEY), is(false));
    }

    private Thread newReaderThread()
    {
        return new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                latch.release();
                lookupObjects = muleContext.getRegistry().lookupByType(Object.class);
                auxiliaryLatch.release();
            }
        });
    }

    private void waitOn(CountDownLatch latch) throws Exception
    {
        assertThat(latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS), is(true));
    }
}
