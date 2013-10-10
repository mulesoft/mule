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
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.functional.CountdownCallback;
import org.mule.tck.functional.FunctionalTestComponent;

public class QuartzDispatchJobTestCase extends AbstractServiceAndFlowTestCase
{

    public QuartzDispatchJobTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "quartz-dispatch-service.xml"},
            {ConfigVariant.FLOW, "quartz-dispatch-flow.xml"}
        });
    }

    @Test
    public void testMuleClientDispatchJob() throws Exception
    {
        FunctionalTestComponent component = getFunctionalTestComponent("scheduledService");
        assertNotNull(component);
        CountdownCallback count = new CountdownCallback(3);
        component.setEventCallback(count);

        new MuleClient(muleContext).dispatch("vm://quartz.scheduler", "quartz test", null);
        assertTrue(count.await(5000));
    }

    @Test
    public void testMuleClientDispatchJobWithExpressionAddress() throws Exception
    {
        FunctionalTestComponent component = getFunctionalTestComponent("expressionScheduledService");
        assertNotNull(component);
        CountdownCallback count = new CountdownCallback(3);
        component.setEventCallback(count);

        Map<String,String> props = new HashMap<String,String>();
        props.put("ENDPOINT_NAME", "quartz.expression.in");

        new MuleClient(muleContext).dispatch("vm://quartz.expression.scheduler", "quartz test", props);
        assertTrue(count.await(5000));
    }
}
