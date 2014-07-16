/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.context.notification.ServerNotification;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.functional.FunctionalTestNotificationListener;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.concurrent.Latch;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class HttpPollingWithTransformersFunctionalTestCase extends AbstractServiceAndFlowTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public HttpPollingWithTransformersFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "mule-http-polling-with-transformers-config-service.xml"},
            {ConfigVariant.FLOW, "mule-http-polling-with-transformers-config-flow.xml"}});
    }

    @Test
    public void testPollingHttpConnector() throws Exception
    {
        final Latch latch = new Latch();
        final AtomicBoolean transformPropagated = new AtomicBoolean(false);
        muleContext.registerListener(new FunctionalTestNotificationListener()
        {
            @Override
            public void onNotification(ServerNotification notification)
            {
                latch.countDown();
                if (notification.getSource().toString().endsWith("toClient-only"))
                {
                    transformPropagated.set(true);
                }
            }
        }, "polledUMO");

        MuleClient client = muleContext.getClient();
        MuleMessage result = client.request("vm://toclient", 50000);
        assertNotNull(result.getPayload());
        assertTrue("Callback called", latch.await(1000, TimeUnit.MILLISECONDS));
        assertEquals("/foo toClient-only", result.getPayloadAsString());
        // The transform should not have been propagated to the outbound endpoint
        assertFalse(transformPropagated.get());
    }
}
