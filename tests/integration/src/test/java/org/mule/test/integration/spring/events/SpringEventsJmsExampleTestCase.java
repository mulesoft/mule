/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.spring.events;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;

import org.mule.MuleManager;
import org.mule.extras.client.MuleClient;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOMessage;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * <code>SpringEventsJmsExampleTestCase</code> is a testcase used to test the
 * example config in the documentation. This test is not run when building this
 * module as it relies on JMS; it's used to verify that the example config works.
 */
public class SpringEventsJmsExampleTestCase extends AbstractMuleTestCase
{
    private static final AtomicInteger eventCount = new AtomicInteger(0);

    private static ClassPathXmlApplicationContext context;

    protected void doSetUp() throws Exception
    {
        if (context == null)
        {
            context = new ClassPathXmlApplicationContext(
                "org/mule/test/integration/spring/events/mule-events-example-app-context.xml");
        }
        else
        {
            context.refresh();
        }

        eventCount.set(0);
    }

    public void testReceivingASubscriptionEvent() throws Exception
    {
        OrderManagerBean subscriptionBean = (OrderManagerBean)context.getBean("orderManager");
        assertNotNull(subscriptionBean);
        // when an event is received by 'testEventBean1' this callback will be
        // invoked
        EventCallback callback = new EventCallback()
        {
            public void eventReceived(UMOEventContext context, Object o) throws Exception
            {
                eventCount.incrementAndGet();
            }
        };
        subscriptionBean.setEventCallback(callback);

        MuleManager.getConfiguration().setSynchronous(true);
        MuleClient client = new MuleClient();
        Order order = new Order("Sausage and Mash");
        client.send("jms://orders.queue", order, null);
        Thread.sleep(1000);
        assertTrue(eventCount.get() == 1);

        UMOMessage result = client.receive("jms://processed.queue", 10000);
        assertEquals(1, eventCount.get());
        assertNotNull(result);
        assertEquals("Order 'Sausage and Mash' Processed", result.getPayload());
    }

    public void testReceiveAsWebService() throws Exception
    {
        MuleClient client = new MuleClient();
        OrderManagerBean orderManager = (OrderManagerBean)context.getBean("orderManager");
        assertNotNull(orderManager);
        EventCallback callback = new EventCallback()
        {
            public void eventReceived(UMOEventContext context, Object o) throws Exception
            {
                eventCount.incrementAndGet();
            }
        };
        orderManager.setEventCallback(callback);

        Order order = new Order("Sausage and Mash");
        UMOMessage result = client.send("axis:http://localhost:44444/mule/orderManager?method=processOrder",
            order, null);

        assertNotNull(result);
        assertEquals("Order 'Sausage and Mash' Processed", (result.getPayload()));
    }

}
