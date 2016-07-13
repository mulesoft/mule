/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.messaging.meps;

import static org.junit.Assert.assertTrue;
import org.mule.functional.functional.FunctionalTestNotificationListener;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.runtime.core.util.concurrent.Latch;

import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

@Ignore("ArtifactClassloaderTestRunner groovy error")
public class InOnlyTestCase extends AbstractIntegrationTestCase
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
        final Latch latch = new Latch();
        muleContext.registerListener(new FunctionalTestNotificationListener()
        {
            @Override
            public void onNotification(ServerNotification notification)
            {
                latch.countDown();
            }
        });

        flowRunner("In-Only-Service").withPayload(getTestMuleMessage()).asynchronously().run();
        assertTrue(latch.await(TIMEOUT, TimeUnit.MILLISECONDS));
    }
}
