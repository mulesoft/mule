/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.registry;

import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.concurrent.Latch;

import java.util.concurrent.TimeUnit;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RegisteredObjectLifecycleTestCase extends AbstractMuleContextTestCase
{
    private static final long TIMEOUT = 1000;

    protected Latch initLatch;
    protected Latch startLatch;
    protected Latch stopLatch;
    protected Latch disposeLatch;

    private DummyBean bean = new DummyBean();

    @Override
    protected void doSetUp() throws Exception
    {
        bean = new DummyBean();
        initLatch = new Latch();
        startLatch = new Latch();
        stopLatch = new Latch();
        disposeLatch = new Latch();
    }

    @Test
    public void testLifecycleForMuleContext() throws Exception
    {
        muleContext.getRegistry().registerObject("dummy", bean);
        assertTrue(initLatch.await(TIMEOUT, TimeUnit.MILLISECONDS));
        assertFalse(startLatch.await(TIMEOUT, TimeUnit.MILLISECONDS));
        assertFalse(stopLatch.await(TIMEOUT, TimeUnit.MILLISECONDS));
        assertFalse(disposeLatch.await(TIMEOUT, TimeUnit.MILLISECONDS));

        muleContext.start();
        assertTrue(startLatch.await(TIMEOUT, TimeUnit.MILLISECONDS));
        assertFalse(stopLatch.await(TIMEOUT, TimeUnit.MILLISECONDS));
        assertFalse(disposeLatch.await(TIMEOUT, TimeUnit.MILLISECONDS));

        muleContext.stop();
        assertTrue(stopLatch.await(TIMEOUT, TimeUnit.MILLISECONDS));
        assertFalse(disposeLatch.await(TIMEOUT, TimeUnit.MILLISECONDS));

        muleContext.dispose();
        assertTrue(disposeLatch.await(TIMEOUT, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testLifecycleForUnregisteredObject() throws Exception
    {
        muleContext.getRegistry().registerObject("dummy", bean);
        assertTrue(initLatch.await(TIMEOUT, TimeUnit.MILLISECONDS));
        assertFalse(startLatch.await(TIMEOUT, TimeUnit.MILLISECONDS));
        assertFalse(stopLatch.await(TIMEOUT, TimeUnit.MILLISECONDS));
        assertFalse(disposeLatch.await(TIMEOUT, TimeUnit.MILLISECONDS));

        muleContext.start();
        assertTrue(startLatch.await(TIMEOUT, TimeUnit.MILLISECONDS));
        assertFalse(stopLatch.await(TIMEOUT, TimeUnit.MILLISECONDS));
        assertFalse(disposeLatch.await(TIMEOUT, TimeUnit.MILLISECONDS));

        muleContext.getRegistry().unregisterObject("dummy");
        assertTrue(stopLatch.await(TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue(disposeLatch.await(TIMEOUT, TimeUnit.MILLISECONDS));
    }

    public class DummyBean implements Lifecycle
    {
        public String echo(String echo)
        {
            return echo;
        }

        public void initialise() throws InitialisationException
        {
            initLatch.countDown();
        }

        public void start() throws MuleException
        {
            startLatch.countDown();
        }

        public void stop() throws MuleException
        {
            stopLatch.countDown();
        }

        public void dispose()
        {
            disposeLatch.countDown();
        }

        public boolean isInitialised()
        {
            return false;
        }

        public boolean isStarted()
        {
            return false;
        }

        public boolean isStopped()
        {
            return false;
        }

        public boolean isDisposed()
        {
            return false;
        }
    }
}
