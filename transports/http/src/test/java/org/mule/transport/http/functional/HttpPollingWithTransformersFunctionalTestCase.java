/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import org.mule.api.MuleMessage;
import org.mule.api.context.notification.ServerNotification;
import org.mule.module.client.MuleClient;
import org.mule.tck.functional.FunctionalTestNotificationListener;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.concurrent.Latch;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HttpPollingWithTransformersFunctionalTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "mule-http-polling-with-transformers-config.xml";
    }

    @Test
    public void testPollingHttpConnector() throws Exception
    {
        final Latch latch = new Latch();
        final AtomicBoolean transformPropagated = new AtomicBoolean(false);
        muleContext.registerListener(new FunctionalTestNotificationListener()
        {
            public void onNotification(ServerNotification notification)
            {
                latch.countDown();
                if(notification.getSource().toString().endsWith("toClient-only"))
                {
                    transformPropagated.set(true);
                }
            }
        }, "polledUMO");

        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.request("vm://toclient", 50000);
        assertNotNull(result.getPayload());
        assertTrue("Callback called", latch.await(1000, TimeUnit.MILLISECONDS));
        assertEquals("/foo toClient-only", result.getPayloadAsString());
        //The transform should not have been propagated to the outbound endpoint
        assertFalse(transformPropagated.get());
    }

}
