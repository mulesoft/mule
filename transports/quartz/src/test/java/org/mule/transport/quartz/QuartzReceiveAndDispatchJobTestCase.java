/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.quartz;

import org.mule.module.client.MuleClient;
import org.mule.tck.functional.CountdownCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class QuartzReceiveAndDispatchJobTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "quartz-receive-dispatch.xml";
    }

    @Test
    public void testMuleClientReceiveAndDispatchJob() throws Exception
    {
        FunctionalTestComponent component = getFunctionalTestComponent("scheduledService");
        assertNotNull(component);
        CountdownCallback count = new CountdownCallback(3);
        component.setEventCallback(count);

        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://event.queue", "quartz test", null);
        client.dispatch("vm://event.queue", "quartz test", null);
        client.dispatch("vm://event.queue", "quartz test", null);

        client.dispatch("vm://quartz.scheduler", "test", null);
        Thread.sleep(5000);
        assertEquals(0, count.getCount());
    }
}
