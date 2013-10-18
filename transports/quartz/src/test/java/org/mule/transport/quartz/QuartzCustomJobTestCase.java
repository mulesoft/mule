/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.quartz;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class QuartzCustomJobTestCase extends AbstractServiceAndFlowTestCase
{

    public QuartzCustomJobTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "quartz-custom-job-service.xml"},
            {ConfigVariant.FLOW, "quartz-custom-job-flow.xml"}
        });
    }

    @Test
    public void testCustomJob() throws Exception
    {
        CountDownLatch eventLatch = (CountDownLatch) muleContext.getRegistry().lookupObject("latch");

        // we wait up to 60 seconds here which is WAY too long for one tick but it seems that 
        // "sometimes" it takes a very long time for Quartz go kick in. Once it starts 
        // ticking everything is fine.
        assertTrue(eventLatch.await(60000, TimeUnit.MILLISECONDS));
    }
    
    public static class MockJob implements Job
    {
        private CountDownLatch eventLatch;

        public MockJob(CountDownLatch latch)
        {
            eventLatch = latch;
        }
        
        public void execute(JobExecutionContext context) throws JobExecutionException
        {
            eventLatch.countDown();
        }
    }
}


