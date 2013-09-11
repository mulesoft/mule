/*
 * $Id\$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.schedule;

import static junit.framework.Assert.assertEquals;

import org.mule.api.schedule.Scheduler;
import org.mule.api.schedule.Schedulers;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;


public class StoppedFixedFrequencyTestCase extends FunctionalTestCase
{

    private static List<String> stoppedFlowResponse = new ArrayList<String>();

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/schedule/poll-scheduler-stopped-config.xml";
    }

    @Test
    public void testStoppedPoll() throws Exception
    {
        runSchedulersOnce();
        Thread.sleep(6000);

        assertEquals(1, stoppedFlowResponse.size());
    }


    public static class FooComponent extends ComponentProcessor
    {

        public FooComponent()
        {
            super(stoppedFlowResponse);
        }
    }

    private void runSchedulersOnce() throws Exception
    {
        Collection<Scheduler> schedulers = muleContext.getRegistry().lookupScheduler(
                Schedulers.flowPollingSchedulers("pollFoo"));

        for (Scheduler scheduler : schedulers)
        {
            scheduler.schedule();
        }
    }
}