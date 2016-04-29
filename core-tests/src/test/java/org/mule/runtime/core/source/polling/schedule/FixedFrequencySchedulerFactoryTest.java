/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.source.polling.schedule;


import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.mule.runtime.core.api.schedule.Scheduler;
import org.mule.runtime.core.source.polling.schedule.FixedFrequencyScheduler;
import org.mule.runtime.core.source.polling.schedule.FixedFrequencySchedulerFactory;
import org.mule.runtime.core.transport.AbstractPollingMessageReceiver;
import org.mule.runtime.core.transport.PollingReceiverWorker;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class FixedFrequencySchedulerFactoryTest
{

    private AbstractPollingMessageReceiver receiver = mock(AbstractPollingMessageReceiver.class);

    @Test
    public void testCreatesCorrectInstance()
    {
        FixedFrequencySchedulerFactory factory = new FixedFrequencySchedulerFactory();
        factory.setFrequency(300);
        factory.setStartDelay(400);
        factory.setTimeUnit(TimeUnit.DAYS);

        Scheduler scheduler = factory.doCreate("name", new PollingReceiverWorker(receiver));

        assertTrue(scheduler instanceof FixedFrequencyScheduler);
        assertEquals(300, ((FixedFrequencyScheduler) scheduler).getFrequency());
        assertEquals(TimeUnit.DAYS, ((FixedFrequencyScheduler) scheduler).getTimeUnit());
        assertEquals("name", scheduler.getName());
    }

    @Test
    public void testDefaultValues()
    {
        FixedFrequencySchedulerFactory factory = new FixedFrequencySchedulerFactory();


        Scheduler scheduler = factory.doCreate("name", new PollingReceiverWorker(receiver));

        assertTrue(scheduler instanceof FixedFrequencyScheduler);
        assertEquals(1000, ((FixedFrequencyScheduler) scheduler).getFrequency());
        assertEquals(TimeUnit.MILLISECONDS, ((FixedFrequencyScheduler) scheduler).getTimeUnit());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeFrequency()
    {
        FixedFrequencySchedulerFactory factory = new FixedFrequencySchedulerFactory();
        factory.setFrequency(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeStartDelay()
    {
        FixedFrequencySchedulerFactory factory = new FixedFrequencySchedulerFactory();
        factory.setStartDelay(-1);
    }
}
