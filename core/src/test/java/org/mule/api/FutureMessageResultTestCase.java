/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
