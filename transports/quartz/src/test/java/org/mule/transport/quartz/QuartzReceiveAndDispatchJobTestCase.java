/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.quartz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.functional.CountdownCallback;
import org.mule.tck.functional.FunctionalTestComponent;

public class QuartzReceiveAndDispatchJobTestCase extends AbstractServiceAndFlowTestCase
{

    public QuartzReceiveAndDispatchJobTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE, "quartz-receive-dispatch-service.xml"},
            {ConfigVariant.FLOW, "quartz-receive-dispatch-flow.xml"}});
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
