/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.issues;

import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.umo.UMOEventContext;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

public class MulticastRouterMule2112TestCase  extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "issues/multicast-router-mule-2112-test.xml";
    }

    public void testMulticastRoutingOverTwoEndpoints() throws Exception
    {
        Object hop1 = getPojoServiceForComponent("hop1");
        assertTrue("FunctionalTestComponent expected", hop1 instanceof FunctionalTestComponent);
        Object hop2 = getPojoServiceForComponent("hop2");
        assertTrue("FunctionalTestComponent expected", hop2 instanceof FunctionalTestComponent);
        assertNotNull(hop1);
        assertNotNull(hop2);

        final AtomicBoolean hop1made = new AtomicBoolean(false);
        EventCallback callback1 = new EventCallback()
        {
            public void eventReceived(final UMOEventContext context, final Object component) throws Exception
            {
                assertTrue(hop1made.compareAndSet(false, true));
            }
        };

        final AtomicBoolean hop2made = new AtomicBoolean(false);
        EventCallback callback2 = new EventCallback()
        {
            public void eventReceived(final UMOEventContext context, final Object component) throws Exception
            {
                assertTrue(hop2made.compareAndSet(false, true));
            }
        };

        ((FunctionalTestComponent) hop1).setEventCallback(callback1);
        ((FunctionalTestComponent) hop2).setEventCallback(callback2);

        MuleClient client = new MuleClient();
        MuleMessage request = new MuleMessage("payload");
        client.dispatch("vm://inbound", request);
        Thread.sleep(1000);

        assertTrue("First callback never fired", hop1made.get());
        assertTrue("Second callback never fired", hop2made.get());
    }

}
