/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.modules.schedulers.cron;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.api.MuleException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.transport.AbstractPollingMessageReceiver;
import org.mule.transport.PollingReceiverWorker;

import org.junit.Before;
import org.junit.Test;


public class CronSchedulerTest  extends AbstractMuleContextTestCase
{

    private AbstractPollingMessageReceiver receiver = mock(AbstractPollingMessageReceiver.class);
    private InboundEndpoint endpoint = mock(InboundEndpoint.class);
    private Prober pollingProber = new PollingProber(1000, 0l);

    @Before
    public void setExpects()
    {
        when(receiver.getEndpoint()).thenReturn(endpoint);
        when(receiver.getReceiverKey()).thenReturn("receiverKey");
        when(endpoint.getName()).thenReturn("endpointName");

    }
    @Test
    public void validateLifecycleHappyPath() throws MuleException
    {
        CronScheduler scheduler = createVoidScheduler();

        scheduler.initialise();
        scheduler.start();
        scheduler.stop();
        scheduler.dispose();
    }

    @Test
    public void stopAfterInitializeShouldNotFail() throws MuleException
    {
        CronScheduler scheduler = createVoidScheduler();
        scheduler.initialise();
        scheduler.stop();
    }


    @Test
    public void startAfterStopShouldNotFail() throws Exception
    {
        final TestPollingWorker job = new TestPollingWorker(receiver);
        CronScheduler scheduler = createScheduler(job);


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
    }

    private CronScheduler createVoidScheduler()
    {
        CronScheduler scheduler = new CronScheduler("name", new PollingReceiverWorker(receiver), "0/1 * * * * ?");
        scheduler.setMuleContext(muleContext);
        return scheduler;
    }

    private CronScheduler createScheduler(PollingReceiverWorker job)
    {
        CronScheduler cronScheduler = new CronScheduler("name", job, "0/1 * * * * ?");
        cronScheduler.setMuleContext(muleContext);
        return cronScheduler;
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
