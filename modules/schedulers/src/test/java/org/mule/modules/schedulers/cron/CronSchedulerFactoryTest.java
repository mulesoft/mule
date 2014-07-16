/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.modules.schedulers.cron;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import org.mule.api.schedule.Scheduler;
import org.mule.transport.AbstractPollingMessageReceiver;
import org.mule.transport.PollingReceiverWorker;

import org.junit.Test;

public class CronSchedulerFactoryTest
{

    private AbstractPollingMessageReceiver receiver = mock(AbstractPollingMessageReceiver.class);

    @Test
    public void testSchedulerCreation()
    {
        CronSchedulerFactory factory = new CronSchedulerFactory();
        factory.setExpression("my expression");

        Scheduler scheduler = factory.create("name", new PollingReceiverWorker(receiver));

        assertTrue(scheduler instanceof CronScheduler);
        assertEquals("my expression", ((CronScheduler) scheduler).getCronExpression());
    }

}
