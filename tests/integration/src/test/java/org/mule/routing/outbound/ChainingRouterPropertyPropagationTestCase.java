/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing.outbound;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ChainingRouterPropertyPropagationTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
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

        MuleClient client = new MuleClient(muleContext);
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
