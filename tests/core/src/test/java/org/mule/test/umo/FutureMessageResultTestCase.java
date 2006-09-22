/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.umo;

import edu.emory.mathcs.backport.java.util.concurrent.Callable;
import edu.emory.mathcs.backport.java.util.concurrent.ExecutionException;
import edu.emory.mathcs.backport.java.util.concurrent.ExecutorService;
import edu.emory.mathcs.backport.java.util.concurrent.Executors;
import edu.emory.mathcs.backport.java.util.concurrent.RejectedExecutionException;
import edu.emory.mathcs.backport.java.util.concurrent.TimeoutException;

import junit.framework.TestCase;

import org.mule.umo.FutureMessageResult;
import org.mule.umo.transformer.TransformerException;

public class FutureMessageResultTestCase extends TestCase
{
    private static Callable Dummy = new Callable()
    {
        public Object call()
        {
            return null;
        }
    };

    volatile boolean wasCalled;

    public FutureMessageResultTestCase(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        wasCalled = false;
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testCreation()
    {
        try
        {
            new FutureMessageResult(null);
            fail();
        }
        catch (NullPointerException npe)
        {
            // OK: see FutureTask(Callable)
        }

        try
        {
            FutureMessageResult f = new FutureMessageResult(Dummy);
            f.setExecutor(null);
            fail();
        }
        catch (IllegalArgumentException iex)
        {
            // OK: no null ExecutorService
        }

    }

    public void testExecute() throws ExecutionException, InterruptedException, TransformerException
    {
        Callable c = new Callable()
        {
            public Object call()
            {
                wasCalled = true;
                return null;
            }
        };

        FutureMessageResult f = new FutureMessageResult(c);
        f.execute();

        assertNull(f.getMessage());
        assertTrue(wasCalled);
    }

    public void testExecuteWithShutdownExecutor()
    {
        ExecutorService e = Executors.newCachedThreadPool();
        e.shutdown();

        FutureMessageResult f = new FutureMessageResult(Dummy);
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

    public void testExecuteWithTimeout()
        throws ExecutionException, InterruptedException, TransformerException
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

        FutureMessageResult f = new FutureMessageResult(c);
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
