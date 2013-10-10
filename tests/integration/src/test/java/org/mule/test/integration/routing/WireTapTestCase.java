/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.routing;

import org.mule.api.context.notification.ServerNotification;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.functional.FunctionalTestNotificationListener;
import org.mule.util.concurrent.Latch;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertTrue;

public class WireTapTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/integration/routing/wire-tap-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/routing/wire-tap-flow.xml"}});
    }

    public WireTapTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testWireTap() throws Exception
    {
        final Latch receiverLatch = new Latch();
        final Latch tappedReceiver1Latch = new Latch();
        final Latch tappedReceiver2Latch = new Latch();
        muleContext.registerListener(new FunctionalTestNotificationListener()
        {
            @Override
            public void onNotification(ServerNotification notification)
            {
                if (notification.getResourceIdentifier().equals("Receiver"))
                {
                    receiverLatch.countDown();
                }
                else if (notification.getResourceIdentifier().equals("TappedReceiver1"))
                {
                    tappedReceiver1Latch.countDown();
                }
                else if (notification.getResourceIdentifier().equals("TappedReceiver2"))
                {
                    tappedReceiver2Latch.countDown();
                }
            }
        });
        MuleClient client = new MuleClient(muleContext);
        client.send("vm://inbound.channel", "test", null);
        assertTrue(receiverLatch.await(3L, TimeUnit.SECONDS));
        assertTrue(tappedReceiver1Latch.await(1L, TimeUnit.SECONDS));
        assertTrue(tappedReceiver2Latch.await(1L, TimeUnit.SECONDS));
    }
}
