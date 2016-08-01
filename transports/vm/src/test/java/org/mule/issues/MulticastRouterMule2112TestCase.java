/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.issues;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.functional.functional.EventCallback;
import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.client.MuleClient;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

public class MulticastRouterMule2112TestCase  extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "issues/multicast-router-mule-2112-test.xml";
    }

    @Test
    public void testMulticastRoutingOverTwoEndpoints() throws Exception
    {
        FunctionalTestComponent hop1 = getFunctionalTestComponent("hop1");
        assertNotNull(hop1);
        FunctionalTestComponent hop2 = getFunctionalTestComponent("hop2");
        assertNotNull(hop2);

        final AtomicBoolean hop1made = new AtomicBoolean(false);
        EventCallback callback1 = new EventCallback()
        {
            @Override
            public void eventReceived(final MuleEventContext context, final Object component) throws Exception
            {
                assertTrue(hop1made.compareAndSet(false, true));
            }
        };

        final AtomicBoolean hop2made = new AtomicBoolean(false);
        EventCallback callback2 = new EventCallback()
        {
            @Override
            public void eventReceived(final MuleEventContext context, final Object component) throws Exception
            {
                assertTrue(hop2made.compareAndSet(false, true));
            }
        };

        hop1.setEventCallback(callback1);
        hop2.setEventCallback(callback2);

        MuleClient client = muleContext.getClient();
        client.send("vm://inbound", "payload", null);
        Thread.sleep(1000);

        assertTrue("First callback never fired", hop1made.get());
        assertTrue("Second callback never fired", hop2made.get());
    }
}
