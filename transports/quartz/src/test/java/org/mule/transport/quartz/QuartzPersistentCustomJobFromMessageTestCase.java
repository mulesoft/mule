/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.quartz;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.quartz.jobs.ScheduledDispatchJobConfig;

public class QuartzPersistentCustomJobFromMessageTestCase extends FunctionalTestCase
{
//    private static final long TIMEOUT = 30000;
    private static final long TIMEOUT = 3000000;

    @Override
    protected String getConfigResources()
    {
        return "quartz-persistent-custom-job-generator.xml";
    }

    public void testSendToCustomEventScheduler() throws Exception
    {
        MuleClient client = new MuleClient();

        ScheduledDispatchJobConfig jobConfig = new ScheduledDispatchJobConfig();
        jobConfig.setEndpointRef("vm://resultQueue");
        client.send("vm://customJobQueue", jobConfig, null);
        
        MuleMessage result = client.request("vm://resultQueue", TIMEOUT);
        assertNotNull(result);
        assertTrue(result.getPayload() instanceof ScheduledDispatchJobConfig);
    }
}
