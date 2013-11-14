/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.spring.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class MuleEventMulticasterTestCase extends FunctionalTestCase
{
    private final AtomicInteger eventCount = new AtomicInteger(0);

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/spring/events/mule-events-example-app-context.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        eventCount.set(0);
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
        client.dispatch("jms://orders.queue", order, null);
        Thread.sleep(2000);
        assertTrue(eventCount.get() == 1);

        MuleMessage result = client.request("jms://processed.queue", 10000);
        assertEquals(1, eventCount.get());
        assertNotNull(result);
        assertEquals("Order 'Sausage and Mash' Processed", result.getPayload());
    }
}
