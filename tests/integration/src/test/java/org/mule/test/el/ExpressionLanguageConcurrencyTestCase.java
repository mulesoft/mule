/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.el;

import static org.junit.Assert.fail;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class ExpressionLanguageConcurrencyTestCase extends AbstractIntegrationTestCase
{
    @Override
    protected String getConfigFile()
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
                        flowRunner("slowRequestHandler").withPayload("foo").run();
                    }
                    catch (Exception e)
                    {
                        // A NullPointerException is thrown when a lookup is performed when a registry is
                        // added or removed concurrently
                        errors.incrementAndGet();
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
        if (errors.get() > 0)
        {
            fail();
        }
    }
}
