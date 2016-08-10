/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.schedule;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.util.Predicate;

import org.junit.Test;

/**
 * This test checks that a Scheduler can be stopped, executed and started. Also shows how a customer can set his own
 * scheduler in mule config.
 *
 * It also shows the way users can add a new Scheduler as a spring bean.
 */
public class RunningScheduleTestCase extends AbstractIntegrationTestCase
{
    public static final String SCHEDULER_NAME = "testScheduler";

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/schedule/scheduler-config.xml";
    }

    @Test
    public void test() throws Exception
    {
        Thread.sleep(2000);

        MockScheduler scheduler = findScheduler(SCHEDULER_NAME);
        assertTrue(scheduler.getCount() > 0);

        stopSchedulers();

        Thread.sleep(2000);

        int count = scheduler.getCount();

        Thread.sleep(2000);

        assertEquals(count, scheduler.getCount());
        scheduler.schedule();

        Thread.sleep(2000);

        assertEquals(count + 1, scheduler.getCount());


    }

    private void stopSchedulers() throws MuleException
    {
        findScheduler(SCHEDULER_NAME).stop();
    }

    private MockScheduler findScheduler(String schedulerName)
    {
        return (MockScheduler) muleContext.getRegistry().lookupScheduler(new NamePredicate(schedulerName)).iterator().next();
    }

    private class NamePredicate implements Predicate<String>
    {

        private String name;

        private NamePredicate(String name)
        {
            this.name = name;
        }

        @Override
        public boolean evaluate(String s)
        {
            return s.equalsIgnoreCase(name);
        }
    }
}
