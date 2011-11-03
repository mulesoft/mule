/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.vm.functional.transactions;



import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.concurrent.Latch;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.junit.Assert;

public class RollbackTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/config/rollback-config.xml";
    }

    static Latch latch;
    static AtomicInteger totalSeen;
    static AtomicInteger totalAccepted;

    @Test
    public void testRollback() throws Exception
    {
        totalSeen = new AtomicInteger(0);
        totalAccepted = new AtomicInteger(0);
        latch = new Latch();
        MuleClient client = new MuleClient(muleContext);
        Map props = new HashMap();
        for (int i = 0; i < 100; i++)
        {
            client.dispatch("vm://async", "Hello " + i, props);
        }
        latch.await();
        Assert.assertEquals(100, totalAccepted.get());
        Assert.assertTrue(totalSeen.get() >= 100);
    }

    public static class AggregatingComponent
    {

        private Random r = new Random(System.currentTimeMillis());

        public void process(String s)
        {
            totalSeen.incrementAndGet();
            int r = this.r.nextInt(10);
            if (r > 8)
            {
                // Fail and roll the tx back 10% of them
                throw new RuntimeException();
            }
            totalAccepted.incrementAndGet();
            if (totalAccepted.get() == 100)
            {
                latch.countDown();
            }
        }
    }
}
