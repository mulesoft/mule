/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.quartz;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.quartz.jobs.ScheduledDispatchJobConfig;

import org.junit.Test;

public class QuartzPersistentCustomJobFromMessageTestCase extends FunctionalTestCase
{
    private static final long TIMEOUT = 30000;

    @Override
    protected String getConfigFile()
    {
        return "quartz-persistent-custom-job-generator-flow.xml";
    }

    @Test
    public void testSendToCustomEventScheduler() throws Exception
    {
        MuleClient client = muleContext.getClient();

        ScheduledDispatchJobConfig jobConfig = new ScheduledDispatchJobConfig();
        jobConfig.setMuleContext(muleContext);
        jobConfig.setEndpointRef("vm://resultQueue");
        client.dispatch("vm://customJobQueue", jobConfig, null);

        MuleMessage result = client.request("vm://resultQueue", TIMEOUT);
        assertNotNull(result);
        assertTrue(result.getPayload() instanceof ScheduledDispatchJobConfig);
    }
}
