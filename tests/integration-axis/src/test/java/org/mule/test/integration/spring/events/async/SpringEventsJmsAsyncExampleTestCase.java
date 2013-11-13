/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.spring.events.async;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.integration.spring.events.Order;
import org.mule.test.integration.spring.events.OrderManagerBean;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Rule;
import org.junit.Test;

/**
 * <code>SpringEventsJmsExampleTestCase</code> is a testcase used to test the
 * example config in the docco. this test is not run when building this module as it
 * relies on Jms, it's used to verify the example config works.
 */
public class SpringEventsJmsAsyncExampleTestCase extends FunctionalTestCase
{
    private final AtomicInteger eventCount = new AtomicInteger(0);

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/spring/events/async/mule-events-example-async-app-context.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        eventCount.set(0);
    }

    @Test
    public void testReceiveAsWebService() throws Exception
    {
        MuleClient client = muleContext.getClient();
        OrderManagerBean orderManager = (OrderManagerBean) muleContext.getRegistry().lookupObject("orderManagerBean");
        assertNotNull(orderManager);
        EventCallback callback = new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object o) throws Exception
            {
                eventCount.incrementAndGet();
            }
        };
        orderManager.setEventCallback(callback);

        Order order = new Order("Sausage and Mash");
        // Make an async call
        client.dispatch("axis:http://localhost:" + dynamicPort.getNumber() + "/mule/orderManager?method=processOrderAsync", order, null);

        MuleMessage result = client.request("jms://processed.queue", 10000);
        assertNotNull(result);
        assertEquals("Order 'Sausage and Mash' Processed Async", result.getPayload());
    }
}
