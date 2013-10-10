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
import org.mule.tck.functional.EventCallback;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.integration.spring.events.Order;
import org.mule.test.integration.spring.events.OrderManagerBean;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
    protected String getConfigResources()
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
        MuleClient client = new MuleClient(muleContext);
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
        // Make an async call
        client.dispatch("axis:http://localhost:" + dynamicPort.getNumber() + "/mule/orderManager?method=processOrderAsync", order, null);

        MuleMessage result = client.request("jms://processed.queue", 10000);
        assertNotNull(result);
        assertEquals("Order 'Sausage and Mash' Processed Async", result.getPayload());
    }

}
