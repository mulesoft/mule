/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.spring.events;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.functional.EventCallback;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MuleEventMulticasterTestCase extends FunctionalTestCase
{

    private final AtomicInteger eventCount = new AtomicInteger(0);

    @Override
    protected String getConfigResources()
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
            public void eventReceived(MuleEventContext context, Object o) throws Exception
            {
                eventCount.incrementAndGet();
            }
        };
        subscriptionBean.setEventCallback(callback);

        MuleClient client = new MuleClient(muleContext);
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
