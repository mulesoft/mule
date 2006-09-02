/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the BSD style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.umo;

import edu.emory.mathcs.backport.java.util.concurrent.Callable;
import edu.emory.mathcs.backport.java.util.concurrent.ExecutionException;
import edu.emory.mathcs.backport.java.util.concurrent.ExecutorService;
import edu.emory.mathcs.backport.java.util.concurrent.Executors;
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

    private volatile boolean wasCalled;

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
            new FutureMessageResult(Dummy, null, null);
            fail();
        }
        catch (IllegalArgumentException iex)
        {
            // OK: no null ExecutorService
        }

        try
        {
            ExecutorService e = Executors.newCachedThreadPool();
            e.shutdown();
            new FutureMessageResult(Dummy, null, e);
            fail();
        }
        catch (IllegalArgumentException iex)
        {
            // OK: no shutdown ExecutorService
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

    public void testExecuteWithTimeout()
        throws ExecutionException, InterruptedException, TransformerException
    {
        Callable c = new Callable()
        {
            public Object call() throws InterruptedException
            {
                // I'm slow, have patience with me
                Thread.sleep(200);
                wasCalled = true;
                return null;
            }
        };

        FutureMessageResult f = new FutureMessageResult(c);
        f.execute();

        try
        {
            assertNull(f.getMessage(50));
        }
        catch (TimeoutException tex)
        {
            // OK: we did not wait long enough for our straggler
        }

        assertFalse(wasCalled);
    }

}
