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
import org.mule.tck.functional.CountdownCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.transport.NullPayload;
import org.mule.transport.quartz.jobs.ScheduledDispatchJobConfig;

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
        FunctionalTestComponent component = (FunctionalTestComponent) getComponent("scheduledService");
        assertNotNull(component);
        CountdownCallback count = new CountdownCallback(1);
        component.setEventCallback(count);

        MuleClient client = new MuleClient();

        Map<String, Object> props = new HashMap<String, Object>();
        ScheduledDispatchJobConfig jobConfig = new ScheduledDispatchJobConfig();
        jobConfig.setEndpointRef("vm://quartz.in");
        props.put(QuartzConnector.PROPERTY_JOB_CONFIG, jobConfig);

        client.send("vm://quartz.scheduler1", NullPayload.getInstance(), props);
        assertTrue(count.await(7000));
        
        // now that the scheduler sent the event, null out the event callback to avoid CountdownCallback
        // report more messages than requested during shutdown of the test/Mule server
        component.setEventCallback(null);
    }

    public void testDelegatingJobAsPayload() throws Exception
    {
        FunctionalTestComponent component = (FunctionalTestComponent) getComponent("scheduledService");
        assertNotNull(component);
        CountdownCallback count = new CountdownCallback(1);
        component.setEventCallback(count);

        MuleClient client = new MuleClient();

        ScheduledDispatchJobConfig jobConfig = new ScheduledDispatchJobConfig();
        jobConfig.setEndpointRef("vm://quartz.in");

        client.send("vm://quartz.scheduler2", jobConfig, null);
        assertTrue(count.await(7000));

        // now that the scheduler sent the event, null out the event callback to avoid CountdownCallback
        // report more messages than requested during shutdown of the test/Mule server
        component.setEventCallback(null);
    }
}
