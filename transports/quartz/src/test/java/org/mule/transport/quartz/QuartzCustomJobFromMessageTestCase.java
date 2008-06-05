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

import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.FunctionalTestComponent2;
import org.mule.tck.functional.CountdownCallback;
import org.mule.transport.quartz.QuartzConnector;
import org.mule.transport.quartz.jobs.ScheduledDispatchJobConfig;
import org.mule.transport.NullPayload;

import java.util.HashMap;
import java.util.Map;

public class QuartzCustomJobFromMessageTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "quartz-receive-dispatch-delegating-job.xml";
    }

    public void testDelegatingJobAsProperty() throws Exception
    {
        FunctionalTestComponent2 component = (FunctionalTestComponent2) getComponent("scheduledService");
        assertNotNull(component);
        CountdownCallback count = new CountdownCallback(1);
        component.setEventCallback(count);

        MuleClient client = new MuleClient();

        Map props = new HashMap();
        ScheduledDispatchJobConfig jobConfig = new ScheduledDispatchJobConfig();
        jobConfig.setEndpointRef("vm://quartz.in");
        props.put(QuartzConnector.PROPERTY_JOB_CONFIG, jobConfig);

        client.send("vm://quartz.scheduler1", NullPayload.getInstance(), props);
        assertTrue(count.await(7000));
    }

    public void testDelegatingJobAsPayload() throws Exception
    {
        FunctionalTestComponent2 component = (FunctionalTestComponent2) getComponent("scheduledService");
        assertNotNull(component);
        CountdownCallback count = new CountdownCallback(1);
        component.setEventCallback(count);


        MuleClient client = new MuleClient();

        ScheduledDispatchJobConfig jobConfig = new ScheduledDispatchJobConfig();
        jobConfig.setEndpointRef("vm://quartz.in");

        client.send("vm://quartz.scheduler2", jobConfig, null);
        assertTrue(count.await(7000));
    }

}
