/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
