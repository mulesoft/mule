/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.quartz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.api.client.MuleClient;
import org.mule.tck.functional.CountdownCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Ignore;
import org.junit.Test;

public class QuartzReceiveAndDispatchJobTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "quartz-receive-dispatch-flow.xml";
    }

    @Test
    @Ignore("MULE-6926")
    public void testMuleClientReceiveAndDispatchJob() throws Exception
    {
        FunctionalTestComponent component = getFunctionalTestComponent("scheduledService");
        assertNotNull(component);
        CountdownCallback count = new CountdownCallback(3);
        component.setEventCallback(count);

        MuleClient client = muleContext.getClient();
        client.dispatch("vm://event.queue", "quartz test", null);
        client.dispatch("vm://event.queue", "quartz test", null);
        client.dispatch("vm://event.queue", "quartz test", null);

        client.dispatch("vm://quartz.scheduler", "test", null);
        Thread.sleep(5000);
        assertEquals(0, count.getCount());
    }
}
