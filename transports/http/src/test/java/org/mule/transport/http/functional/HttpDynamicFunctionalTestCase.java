/*
 * $Id$
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.functional;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.DynamicPortTestCase;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.util.concurrent.Latch;

import java.util.HashMap;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class HttpDynamicFunctionalTestCase extends DynamicPortTestCase
{
    protected static String TEST_MESSAGE = "Test Http Request";

    protected String getConfigResources()
    {
        return "http-dynamic-functional-test.xml";
    }

    public void testSend() throws Exception
    {
        final Latch latch1 = new Latch();
        final Latch latch2 = new Latch();
        FunctionalTestComponent tc1 = getFunctionalTestComponent("testComponent1");
        FunctionalTestComponent tc2 = getFunctionalTestComponent("testComponent2");
        assertNotNull(tc1);
        assertNotNull(tc2);

            tc1.setEventCallback(new EventCallback() {
                public void eventReceived(MuleEventContext context, Object component) throws Exception
                {
                    latch1.release();
                }
            });
            tc2.setEventCallback(new EventCallback(){
                public void eventReceived(MuleEventContext context, Object component) throws Exception
                {
                    latch2.release();
                }
            });

        MuleClient client = new MuleClient(muleContext);
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("port", getPorts().get(0));
        props.put("path", "foo");
        MuleMessage result = client.send("clientEndpoint", TEST_MESSAGE, props);
        assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());

        assertTrue(latch1.await(3000, TimeUnit.MILLISECONDS));

        props.put("port", getPorts().get(1));
        result = client.send("clientEndpoint", TEST_MESSAGE, props);
        assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());

        assertTrue(latch2.await(3000, TimeUnit.MILLISECONDS));
    }

    @Override
    protected int getNumPortsToFind()
    {
        return 2;
    }
}
