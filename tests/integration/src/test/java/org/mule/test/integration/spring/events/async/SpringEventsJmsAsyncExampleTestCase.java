/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.spring.events.async;

import org.mule.RegistryContext;
import org.mule.extras.client.MuleClient;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.test.integration.spring.events.Order;
import org.mule.test.integration.spring.events.OrderManagerBean;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOMessage;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * <code>SpringEventsJmsExampleTestCase</code> is a testcase used to test the
 * example config in the docco. this test is not run when building this module as it
 * relies on Jms, it's used to verify the example config works.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class SpringEventsJmsAsyncExampleTestCase extends AbstractMuleTestCase
{
    private static int eventCount = 0;
    private static ClassPathXmlApplicationContext context;

    protected void doSetUp() throws Exception
    {
        if (context == null)
        {
            context = new ClassPathXmlApplicationContext(
                "org/mule/test/integration/spring/events/async/mule-events-example-async-app-context.xml");
        }
        else
        {
            context.refresh();
        }
        eventCount = 0;
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
                eventCount++;
            }
        };
        subscriptionBean.setEventCallback(callback);

        RegistryContext.getConfiguration().setDefaultSynchronousEndpoints(true);
        MuleClient client = new MuleClient();
        Order order = new Order("Sausage and Mash");
        client.send("jms://orders.queue", order, null);
        Thread.sleep(1000);
        assertTrue(eventCount == 1);

        UMOMessage result = client.receive("jms://processed.queue", 10000);
        assertEquals(1, eventCount);
        assertNotNull(result);
        assertEquals("Order 'Sausage and Mash' Processed", result.getPayloadAsString());
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
                eventCount++;
            }
        };
        orderManager.setEventCallback(callback);

        Order order = new Order("Sausage and Mash");
        // Make an async call
        client.dispatch("axis:http://localhost:44444/mule/orderManager?method=processOrderAsync", order, null);

        UMOMessage result = client.receive("jms://processed.queue", 10000);
        assertNotNull(result);
        assertEquals("Order 'Sausage and Mash' Processed Async", result.getPayload());
    }
}
