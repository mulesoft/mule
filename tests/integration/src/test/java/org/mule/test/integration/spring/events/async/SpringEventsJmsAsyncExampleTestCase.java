/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.spring.events.async;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.test.integration.spring.events.Order;
import org.mule.test.integration.spring.events.OrderManagerBean;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * <code>SpringEventsJmsExampleTestCase</code> is a testcase used to test the
 * example config in the docco. this test is not run when building this module as it
 * relies on Jms, it's used to verify the example config works.
 */
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
    protected String getConfigResources()
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
            public void eventReceived(MuleEventContext context, Object o) throws Exception
            {
                eventCount.incrementAndGet();
            }
        };
        subscriptionBean.setEventCallback(callback);

        MuleClient client = new MuleClient(muleContext);
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
