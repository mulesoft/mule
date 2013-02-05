/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.el;

import static org.junit.Assert.fail;

import org.mule.tck.junit4.FunctionalTestCase;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class ExpressionLanguageConcurrencyTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/el/expression-language-concurrency-config.xml";
    }

    @Test
    public void testConcurrentEvaluation() throws Exception
    {
        final int N = 100;
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch end = new CountDownLatch(N);
        final AtomicInteger errors = new AtomicInteger(0);
        for (int i = 0; i < N; i++)
        {
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        start.await();
                        // System.out.println("...evaluating thread " + Thread.currentThread().getName() +
                        // "...");
                        System.out.println(String.format(">>>>>>>> before thread %s -> %s",
                            Thread.currentThread().getName(), new Date()));
                        testFlow("slowRequestHandler", getTestEvent("foo"));
                        System.out.println(String.format("+++++++++ after thread %s -> %s",
                            Thread.currentThread().getName(), new Date()));
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        errors.incrementAndGet();
                        System.out.println("\n\n incremented error count to " + errors.get());
                    }
                    finally
                    {
                        end.countDown();
                    }
                }
            }, "thread-eval-" + i).start();
        }
        start.countDown();
        end.await();
        System.out.println(String.format("end loop -> %s", new Date()));
        System.out.println("\n\n final error count " + errors.get());
        if (errors.get() > 0)
        {
            fail();
        }
    }
}
