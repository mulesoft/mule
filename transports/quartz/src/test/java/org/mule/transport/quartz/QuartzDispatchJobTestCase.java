/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
