/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.spring.events.async;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.test.integration.spring.events.Order;
import org.mule.test.integration.spring.events.OrderManagerBean;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

/**
 * <code>SpringEventsJmsExampleTestCase</code> is a testcase used to test the
 * example config in the docco. this test is not run when building this module as it
 * relies on Jms, it's used to verify the example config works.
 */
@Ignore("MULE-4976 (broken with migration to Spring 3)")
public class SpringEventsJmsAsyncExampleTestCase extends FunctionalTestCase
{
    private final AtomicInteger eventCount = new AtomicInteger(0);

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        eventCount.set(0);
    }

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/spring/events/async/mule-events-example-async-app-context.xml";
    }

    @Test
    public void testReceivingASubscriptionEvent() throws Exception
    {
        OrderManagerBean subscriptionBean = (OrderManagerBean) muleContext.getRegistry().lookupObject(
            "orderManagerBean");
        assertNotNull(subscriptionBean);
        // when an event is received by 'testEventBean1' this callback will be
        // invoked
        EventCallback callback = new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object o) throws Exception
            {
                eventCount.incrementAndGet();
            }
        };
        subscriptionBean.setEventCallback(callback);

        MuleClient client = muleContext.getClient();
        Order order = new Order("Sausage and Mash");
        client.send("jms://orders.queue", order, null);
        Thread.sleep(1000);
        assertTrue(eventCount.get() == 1);

        MuleMessage result = client.request("jms://processed.queue", 10000);
        assertEquals(1, eventCount.intValue());
        assertNotNull(result);
        assertEquals("Order 'Sausage and Mash' Processed", result.getPayloadAsString());
    }
}
