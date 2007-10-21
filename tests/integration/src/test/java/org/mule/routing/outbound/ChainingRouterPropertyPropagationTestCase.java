/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.outbound;

import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.model.UMOModel;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

public class ChainingRouterPropertyPropagationTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/outbound/chaining-router-properties-propagation-config.xml";
    }

    public void testPropertiesPropagation() throws Exception
    {
        FunctionalTestComponent hop1 = (FunctionalTestComponent) managementContext.getRegistry().lookupComponent("hop1Service").getServiceFactory().getOrCreate();
        FunctionalTestComponent hop2 = (FunctionalTestComponent) managementContext.getRegistry().lookupComponent("hop2Service").getServiceFactory().getOrCreate();
        assertNotNull(hop1);

        final AtomicBoolean hop1made = new AtomicBoolean(false);
        EventCallback callback1 = new EventCallback()
        {
            public void eventReceived(final UMOEventContext context, final Object component) throws Exception
            {
                UMOMessage msg = context.getMessage();
                assertTrue(hop1made.compareAndSet(false, true));
                FunctionalTestComponent ftc = (FunctionalTestComponent) component;
                ftc.setReturnMessage("Hop1 ACK");
            }
        };

        final AtomicBoolean hop2made = new AtomicBoolean(false);
        EventCallback callback2 = new EventCallback()
        {
            public void eventReceived(final UMOEventContext context, final Object component) throws Exception
            {
                UMOMessage msg = context.getMessage();
                assertTrue(hop2made.compareAndSet(false, true));
                assertEquals("Property not propagated from the first hop.", "hop1", msg.getProperty("TICKET"));
                FunctionalTestComponent ftc = (FunctionalTestComponent) component;
                ftc.setReturnMessage(msg.getPayload() + " Hop2 ACK");
            }
        };

        hop1.setEventCallback(callback1);
        hop2.setEventCallback(callback2);

        MuleClient client = new MuleClient();
        MuleMessage request = new MuleMessage("payload");
        UMOMessage reply = client.send("vm://inbound", request);
        assertNotNull(reply);

        assertTrue("First callback never fired", hop1made.get());
        assertTrue("Second callback never fired", hop2made.get());
        assertEquals("Hop1 ACK Hop2 ACK", reply.getPayload());
        assertEquals("hop1", reply.getProperty("TICKET"));
        assertEquals("10000", reply.getProperty("TTL"));
    }

}
