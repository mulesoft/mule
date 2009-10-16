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

import java.util.HashMap;
import java.util.Map;

public class QuartzDispatchJobTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources() 
    {
        return "quartz-dispatch.xml";
    }

    public void testMuleClientDispatchJob() throws Exception 
    {
        FunctionalTestComponent component = (FunctionalTestComponent) getComponent("scheduledService");
        assertNotNull(component);
        CountdownCallback count = new CountdownCallback(3);
        component.setEventCallback(count);

        new MuleClient().send("vm://quartz.scheduler", "quartz test", null);
        assertTrue(count.await(5000));
    }

    public void testMuleClientDispatchJobWithExpressionAddress() throws Exception 
    {
        FunctionalTestComponent component = (FunctionalTestComponent) getComponent("expressionScheduledService");
        assertNotNull(component);
        CountdownCallback count = new CountdownCallback(3);
        component.setEventCallback(count);

        Map<String,String> props = new HashMap<String,String>();
        props.put("ENDPOINT_NAME", "quartz.expression.in");

        new MuleClient().send("vm://quartz.expression.scheduler", "quartz test", props);
        assertTrue(count.await(5000));
    }
}
