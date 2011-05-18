/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.spring.events;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.DynamicPortTestCase;
import org.mule.tck.functional.EventCallback;

import java.util.concurrent.atomic.AtomicInteger;

public class MuleEventMulticasterTestCase extends DynamicPortTestCase
{
    final AtomicInteger eventCount = new AtomicInteger(0);

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

    public void testReceiveAsWebService() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        OrderManagerBean orderManager = muleContext.getRegistry().get("orderManagerBean");
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
        MuleMessage result = client.send("axis:http://localhost:" + getPorts().get(0) + "/mule/orderManager?method=processOrder", order,
            null);

        assertNotNull(result);
        assertEquals("Order 'Sausage and Mash' Processed", (result.getPayload()));
    }

    @Override
    protected int getNumPortsToFind()
    {
        return 1;
    }
}
