/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.polling.schedule;


import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import org.mule.api.MuleException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.transport.AbstractPollingMessageReceiver;
import org.mule.transport.PollingReceiverWorker;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class FixedFrequencySchedulerTest extends AbstractMuleContextTestCase
{

    private AbstractPollingMessageReceiver receiver = mock(AbstractPollingMessageReceiver.class);
    private Prober pollingProber = new PollingProber(1000, 0l);

    @Test
    public void validateLifecycleHappyPath() throws MuleException
    {
        FixedFrequencyScheduler scheduler = createVoidScheduler();

        scheduler.initialise();
        scheduler.start();
        scheduler.stop();
        scheduler.dispose();
    }

    @Test
    public void stopAfterInitializeShouldNotFail() throws MuleException
    {
        FixedFrequencyScheduler scheduler = createVoidScheduler();
        scheduler.initialise();
        scheduler.stop();
    }


    @Test
    public void startAfterStopShouldNotFail() throws Exception
    {
        final TestPollingWorker job = new TestPollingWorker(receiver);
        FixedFrequencyScheduler scheduler = createScheduler(job);


        scheduler.initialise();
        scheduler.start();
        scheduler.stop();
        scheduler.start();


        assertFalse(job.wasRun);

        scheduler.schedule();

        pollingProber.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                return job.wasRun;
            }

            @Override
            public String describeFailure()
            {
                return "The scheduler was never run";
            }
        });

        scheduler.stop();
    }

    private FixedFrequencyScheduler createVoidScheduler()
    {
        return new FixedFrequencyScheduler("name", 10, 50, new PollingReceiverWorker(receiver), TimeUnit.HOURS);
    }

    private FixedFrequencyScheduler createScheduler(PollingReceiverWorker job)
    {
        return new FixedFrequencyScheduler("name", 10, 50, job, TimeUnit.HOURS);
    }

    private class TestPollingWorker extends PollingReceiverWorker
    {

        boolean wasRun;

        public TestPollingWorker(AbstractPollingMessageReceiver pollingMessageReceiver)
        {
            super(pollingMessageReceiver);
        }

        @Override
        public void run()
        {
            wasRun = true;
        }
    }

}
