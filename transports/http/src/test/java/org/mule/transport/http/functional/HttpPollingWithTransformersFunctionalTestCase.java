/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.MuleMessage;
import org.mule.api.context.notification.ServerNotification;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.functional.FunctionalTestNotificationListener;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.concurrent.Latch;

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
            public void onNotification(ServerNotification notification)
            {
                latch.countDown();
                if (notification.getSource().toString().endsWith("toClient-only"))
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
        // The transform should not have been propagated to the outbound endpoint
        assertFalse(transformPropagated.get());
    }

}
