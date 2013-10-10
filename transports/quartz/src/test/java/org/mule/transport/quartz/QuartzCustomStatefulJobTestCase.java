/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

public class QuartzCustomStatefulJobTestCase extends AbstractServiceAndFlowTestCase
{
    public QuartzCustomStatefulJobTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

 
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "quartz-custom-stateful-job-service.xml"},
            {ConfigVariant.FLOW, "quartz-custom-stateful-job-flow.xml"}
        });
    }
    
    @Test
    public void testCustomStatefulJob() throws Exception
    {
        CountDownLatch eventLatch = (CountDownLatch) muleContext.getRegistry().lookupObject("latch");

        // we wait up to 60 seconds here which is WAY too long for one tick but it seems that 
        // "sometimes" it takes a very long time for Quartz go kick in. Once it starts 
        // ticking everything is fine.
        assertTrue(eventLatch.await(60000, TimeUnit.MILLISECONDS));
    }

    public static class MyStatefulJob implements StatefulJob
    {
        private CountDownLatch latch;

        public MyStatefulJob(CountDownLatch latch)
        {
            super();
            this.latch = latch;
        }
        
        public void execute(JobExecutionContext context) throws JobExecutionException
        {
            assertTrue(context.getJobDetail().isStateful());
            latch.countDown();
        }
    }
}
