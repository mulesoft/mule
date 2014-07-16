/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.quartz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.functional.CountdownCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.PollingController;

import org.junit.Test;

public class QuartzPollingFunctionalTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "quartz-polling-functional-test.xml";
    }

    @Test
    public void testMuleReceiverJob() throws Exception
    {
        ((DefaultMuleContext) muleContext).setPollingController(new PollingController()
        {
            @Override
            public boolean isPrimaryPollingInstance()
            {
                return false;
            }
        });

        FunctionalTestComponent component = (FunctionalTestComponent) getComponent("quartzService1");
        assertNotNull(component);
        CountdownCallback count1 = new CountdownCallback(1000);
        component.setEventCallback(count1);

        component = (FunctionalTestComponent) getComponent("quartzService2");
        assertNotNull(component);
        CountdownCallback count2 = new CountdownCallback(1000);
        component.setEventCallback(count2);
        Thread.sleep(10000);

        assertEquals(1000, count1.getCount());
        assertEquals(1000, count2.getCount());
        ((DefaultMuleContext)muleContext).setPollingController(new PollingController()
        {
            @Override
            public boolean isPrimaryPollingInstance()
            {
                return true;
            }
        });
        Thread.sleep(5000);
        assertTrue(count1.getCount() < 1000);
        assertTrue(count2.getCount() < 1000);
    }

    @Test
    public void testMuleSenderJob() throws Exception
    {
        ((DefaultMuleContext) muleContext).setPollingController(new PollingController()
        {
            @Override
            public boolean isPrimaryPollingInstance()
            {
                return false;
            }
        });

        MuleClient client = muleContext.getClient();
        client.dispatch("vm://source", "Hello", null);
        Thread.sleep(10000);
        int numMessages = -1;
        MuleMessage message;
        do
        {
            numMessages++;
            message = client.request("vm://sink", 1);
            if (message != null)
            {
                assertEquals("Hello", message.getPayload());
            }
        }
        while (message != null);
        assertTrue(numMessages > 0);
    }

}