/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing;

import static org.junit.Assert.assertTrue;
import org.mule.functional.functional.FunctionalTestNotificationListener;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.runtime.core.util.concurrent.Latch;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class WireTapTestCase extends AbstractIntegrationTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/routing/wire-tap-flow.xml";
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
        flowRunner("Receiver").withPayload(TEST_PAYLOAD).run();
        assertTrue(receiverLatch.await(3L, TimeUnit.SECONDS));
        assertTrue(tappedReceiver1Latch.await(1L, TimeUnit.SECONDS));
        assertTrue(tappedReceiver2Latch.await(1L, TimeUnit.SECONDS));
    }
}
