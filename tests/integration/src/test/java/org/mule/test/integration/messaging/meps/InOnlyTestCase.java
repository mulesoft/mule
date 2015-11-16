/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.messaging.meps;

import static org.junit.Assert.assertTrue;
import org.mule.api.client.MuleClient;
import org.mule.api.context.notification.ServerNotification;
import org.mule.tck.functional.FunctionalTestNotificationListener;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.concurrent.Latch;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class InOnlyTestCase extends FunctionalTestCase
{
    public static final long TIMEOUT = 3000;

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/messaging/meps/pattern_In-Only-flow.xml";
    }

    @Test
    public void testExchange() throws Exception
    {
        MuleClient client = muleContext.getClient();

        final Latch latch = new Latch();
        muleContext.registerListener(new FunctionalTestNotificationListener()
        {
            @Override
            public void onNotification(ServerNotification notification)
            {
                latch.countDown();
            }
        });

        client.dispatch("inboundEndpoint", "some data", null);
        assertTrue(latch.await(TIMEOUT, TimeUnit.MILLISECONDS));
    }
}
