/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.quartz;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.transport.quartz.jobs.ScheduledDispatchJobConfig;

public class QuartzPersistentCustomJobFromMessageTestCase extends AbstractServiceAndFlowTestCase
{

    private static final long TIMEOUT = 30000;

    public QuartzPersistentCustomJobFromMessageTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "quartz-persistent-custom-job-generator-service.xml"},
            {ConfigVariant.FLOW, "quartz-persistent-custom-job-generator-flow.xml"}});
    }

    @Test
    public void testSendToCustomEventScheduler() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        ScheduledDispatchJobConfig jobConfig = new ScheduledDispatchJobConfig();
        jobConfig.setMuleContext(muleContext);
        jobConfig.setEndpointRef("vm://resultQueue");
        client.dispatch("vm://customJobQueue", jobConfig, null);

        MuleMessage result = client.request("vm://resultQueue", TIMEOUT);
        assertNotNull(result);
        assertTrue(result.getPayload() instanceof ScheduledDispatchJobConfig);
    }
}
