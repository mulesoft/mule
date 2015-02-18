/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing;

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

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class WireTapCxfTestCase extends AbstractServiceAndFlowTestCase
{
    private static final Latch tapLatch = new Latch();

    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/integration/routing/wire-tap-cxf-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/routing/wire-tap-cxf-flow.xml"}});
    }

    public WireTapCxfTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        muleContext.registerListener(new FunctionalTestNotificationListener()
        {
            @Override
            public void onNotification(ServerNotification notification)
            {
                tapLatch.release();
            }
        });
    }

    @Test
    public void testWireTap() throws Exception
    {
        String url = "http://localhost:" + port1.getNumber() + "/services/EchoUMO";
        String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                     + "<soap:Body><echo><text>foo</text></echo></soap:Body></soap:Envelope>";

        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send(url, getTestMuleMessage(msg));
        assertNotNull(response);

        String responseString = response.getPayloadAsString();
        assertTrue(responseString.contains("echoResponse"));
        assertFalse(responseString.contains("soap:Fault"));

        assertTrue(tapLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
    }
}
