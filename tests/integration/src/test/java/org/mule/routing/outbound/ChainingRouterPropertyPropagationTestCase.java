/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

@Ignore("These tests have a property propagation / MEPs issue")
public class ChainingRouterPropertyPropagationTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/routing/outbound/chaining-router-properties-propagation-config.xml";
    }

    @Test
    public void testPropertiesPropagation() throws Exception
    {
        FunctionalTestComponent hop1 = getFunctionalTestComponent("hop1Service");
        FunctionalTestComponent hop2 = getFunctionalTestComponent("hop2Service");
        assertNotNull(hop1);

        final AtomicBoolean hop1made = new AtomicBoolean(false);
        EventCallback callback1 = new EventCallback()
        {
            @Override
            public void eventReceived(final MuleEventContext context, final Object component) throws Exception
            {
                assertTrue(hop1made.compareAndSet(false, true));
                FunctionalTestComponent ftc = (FunctionalTestComponent) component;
                ftc.setReturnData("Hop1 ACK");
            }
        };

        final AtomicBoolean hop2made = new AtomicBoolean(false);
        EventCallback callback2 = new EventCallback()
        {
            @Override
            public void eventReceived(final MuleEventContext context, final Object component) throws Exception
            {
                MuleMessage msg = context.getMessage();
                assertTrue(hop2made.compareAndSet(false, true));
                // this is a service callback, props are on the inbound
                assertEquals("Property not propagated from the first hop.", "hop1", msg.getInboundProperty("TICKET"));
                FunctionalTestComponent ftc = (FunctionalTestComponent) component;
                ftc.setReturnData(msg.getPayload() + " Hop2 ACK");
            }
        };

        hop1.setEventCallback(callback1);
        hop2.setEventCallback(callback2);

        MuleClient client = muleContext.getClient();
        DefaultMuleMessage request = new DefaultMuleMessage("payload", muleContext);
        MuleMessage reply = client.send("inboundEndpoint", request);
        assertNotNull(reply);

        assertTrue("First callback never fired", hop1made.get());
        assertTrue("Second callback never fired", hop2made.get());
        assertEquals("Hop1 ACK Hop2 ACK", reply.getPayload());
        assertEquals("hop1", reply.getInboundProperty("TICKET"));
        assertEquals("10000", reply.getInboundProperty("TTL"));
    }
}
