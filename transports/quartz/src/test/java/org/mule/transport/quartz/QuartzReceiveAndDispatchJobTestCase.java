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

public class QuartzReceiveAndDispatchJobTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "quartz-receive-dispatch.xml";
    }

    public void testMuleClientReceiveAndDispatchJob() throws Exception
    {
        FunctionalTestComponent component = (FunctionalTestComponent)getComponent("scheduledService");
        assertNotNull(component);
        CountdownCallback count = new CountdownCallback(3);
        component.setEventCallback(count);

        MuleClient client = new MuleClient();
        client.dispatch("vm://event.queue", "quartz test", null);
        client.dispatch("vm://event.queue", "quartz test", null);
        client.dispatch("vm://event.queue", "quartz test", null);

        client.dispatch("vm://quartz.scheduler", "test", null);
        assertTrue(count.await(5000));
    }
}
