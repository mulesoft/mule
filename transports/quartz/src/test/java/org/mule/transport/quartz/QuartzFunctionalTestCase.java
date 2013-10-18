/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.quartz;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.functional.CountdownCallback;
import org.mule.tck.functional.FunctionalTestComponent;

public class QuartzFunctionalTestCase extends AbstractServiceAndFlowTestCase
{

    public QuartzFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "quartz-functional-test-service.xml"},
            {ConfigVariant.FLOW, "quartz-functional-test-flow.xml"}
        });
    }

    @Test
    public void testMuleReceiverJob() throws Exception
    {
        FunctionalTestComponent component = (FunctionalTestComponent) getComponent("quartzService1");
        assertNotNull(component);
        CountdownCallback count1 = new CountdownCallback(4);
        component.setEventCallback(count1);

        component = (FunctionalTestComponent) getComponent("quartzService2");
        assertNotNull(component);
        CountdownCallback count2 = new CountdownCallback(2);
        component.setEventCallback(count2);

        // we wait up to 60 seconds here which is WAY too long for three ticks with 1
        // second interval, but it seems that "sometimes" it takes a very long time
        // for Quartz go kick in. Once it starts ticking everything is fine.
        assertTrue("Count 1 timed out: expected 0, value is: " + count1.getCount(), count1.await(60000));
        assertTrue("Count 2 timed out: expected 0, value is: " + count2.getCount(), count2.await(5000));
    }

}
