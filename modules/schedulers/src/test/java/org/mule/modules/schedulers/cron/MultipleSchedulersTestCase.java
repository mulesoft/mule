/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.modules.schedulers.cron;

import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.util.concurrent.Latch;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class MultipleSchedulersTestCase extends FunctionalTestCase
{

    private static CountDownLatch firstRequest = new CountDownLatch(2);
    private static Latch stoppedFlowLatch = new Latch();
    private static int counter = 0;

    @Override
    protected String getConfigFile()
    {
        return "multiple-schedulers-config.xml";
    }

    @Test
    public void schedulersAreNotSharedAcrossPollers() throws Exception
    {
        firstRequest.await(getTestTimeoutSecs(), TimeUnit.SECONDS);

        Flow poll1 = (Flow) muleContext.getRegistry().lookupFlowConstruct("poll1");
        poll1.stop();

        stoppedFlowLatch.countDown();

        PollingProber pollingProber = new PollingProber(5000, 100);
        pollingProber.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                return counter == 2;
            }

            @Override
            public String describeFailure()
            {
                return "Poll2 was not executed after stopping Poll1 flow";
            }
        });

    }

    public static class SynchronizedPollExecutionCounter
    {

        public Object process(Object payload) throws InterruptedException
        {
            if ("poll2".equals(payload))
            {
                counter++;
            }

            firstRequest.countDown();
            stoppedFlowLatch.await();

            return payload;
        }
    }
}
