/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.spring.events;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.EventCallback;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;

/**
 * <code>SpringEventsJmsExampleTestCase</code> is a testcase used to test the
 * example config in the documentation. This test is not run when building this
 * module as it relies on JMS; it's used to verify that the example config works.
 */
public class SpringEventsJmsExampleTestCase extends FunctionalTestCase
{
    private final AtomicInteger eventCount = new AtomicInteger(0);

    protected String getConfigResources()
    {
        return "org/mule/test/integration/spring/events/mule-events-example-app-context.xml";
    }

    public SpringEventsJmsExampleTestCase()
    {
        setStartContext(false);
    }

    protected void doSetUp() throws Exception
    {
        eventCount.set(0);
    }

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

        MuleClient client = new MuleClient();
        Order order = new Order("Sausage and Mash");
        client.dispatch("jms://orders.queue", order, null);
        Thread.sleep(2000);
        assertTrue(eventCount.get() == 1);

        MuleMessage result = client.request("jms://processed.queue", 10000);
        assertEquals(1, eventCount.get());
        assertNotNull(result);
        assertEquals("Order 'Sausage and Mash' Processed", result.getPayload());
    }

    public void testReceiveAsWebService() throws Exception
    {
        MuleClient client = new MuleClient();
        OrderManagerBean orderManager = (OrderManagerBean) muleContext.getRegistry().lookupObject("orderManagerBean");
        assertNotNull(orderManager);
        EventCallback callback = new EventCallback()
        {
            public void eventReceived(MuleEventContext context, Object o) throws Exception
            {
                eventCount.incrementAndGet();
            }
        };
        orderManager.setEventCallback(callback);

        Order order = new Order("Sausage and Mash");
        MuleMessage result = client.send("axis:http://localhost:44444/mule/orderManager?method=processOrder", order,
            null);

        assertNotNull(result);
        assertEquals("Order 'Sausage and Mash' Processed", (result.getPayload()));
    }

}
