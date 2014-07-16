/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api;

import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FutureMessageResultTestCase extends AbstractMuleContextTestCase
{
    private static Callable Dummy = new Callable()
    {
        public Object call()
        {
            return null;
        }
    };

    private volatile boolean wasCalled;

    @Test
    public void testCreation()
    {
        try
        {
            new FutureMessageResult(null, muleContext);
            fail();
        }
        catch (NullPointerException npe)
        {
            // OK: see FutureTask(Callable)
        }

        try
        {
            FutureMessageResult f = new FutureMessageResult(Dummy, muleContext);
            f.setExecutor(null);
            fail();
        }
        catch (IllegalArgumentException iex)
        {
            // OK: no null ExecutorService
        }

    }

    @Test
    public void testExecute() throws ExecutionException, InterruptedException, MuleException
    {
        Callable c = new Callable()
        {
            public Object call()
            {
                wasCalled = true;
                return null;
            }
        };

        FutureMessageResult f = new FutureMessageResult(c, muleContext);
        f.execute();

        assertNull(f.getMessage());
        assertTrue(wasCalled);
    }

    @Test
    public void testExecuteWithShutdownExecutor()
    {
        ExecutorService e = Executors.newCachedThreadPool();
        e.shutdown();

        FutureMessageResult f = new FutureMessageResult(Dummy, muleContext);
        f.setExecutor(e);

        try
        {
            f.execute();
            fail();
        }
        catch (RejectedExecutionException rex)
        {
            // OK: fail with shutdown Executor
        }
    }

    @Test
    public void testExecuteWithTimeout()
        throws ExecutionException, InterruptedException, MuleException
    {
        Callable c = new Callable()
        {
            public Object call() throws InterruptedException
            {
                // I'm slow, have patience with me
                Thread.sleep(3000L);
                wasCalled = true;
                return null;
            }
        };

        FutureMessageResult f = new FutureMessageResult(c, muleContext);
        f.execute();

        try
        {
            f.getMessage(500L);
            fail();
        }
        catch (TimeoutException tex)
        {
            // OK: we did not wait long enough for our straggler, so let's tell him
            // to forget about his task
            f.cancel(true);
        }
        finally
        {
            assertFalse(wasCalled);
        }
    }

}
