/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.routing;

import org.mule.api.context.notification.RoutingNotificationListener;
import org.mule.context.notification.RoutingNotification;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.FunctionalTestComponent;

import java.util.Arrays;
import java.util.List;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;

public class CollectionAggregatorRouterTimeoutTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "collection-aggregator-router-timeout-test.xml";
    }

    public void testNoFailOnTimeout() throws Exception
    {
        // correlation timeouts should not fire in this scenario, check it
        final AtomicInteger correlationTimeoutCount = new AtomicInteger(0);
        muleContext.registerListener(new RoutingNotificationListener<RoutingNotification>()
        {
            public void onNotification(RoutingNotification notification)
            {
                if (notification.getAction() == RoutingNotification.CORRELATION_TIMEOUT)
                {
                    correlationTimeoutCount.incrementAndGet();
                }
            }
        });

        FunctionalTestComponent vortex = (FunctionalTestComponent) getComponent("vortex");

        FunctionalTestComponent aggregator = (FunctionalTestComponent) getComponent("aggregator");

        MuleClient client = new MuleClient();
        List list = Arrays.asList("first", "second");
        client.dispatch("vm://splitter", list, null);

        Thread.sleep(3000);

        // no correlation timeout should ever fire
        assertEquals("Correlation timeout should not have happened.", 0, correlationTimeoutCount.intValue());

        // should receive only the second message
        assertEquals("Vortex received wrong number of messages.", 1, vortex.getReceivedMessagesCount());
        assertEquals("Wrong message received", "second", vortex.getLastReceivedMessage());

        // should receive only the first part
        assertEquals("Aggregator received wrong number of messages.", 1, aggregator.getReceivedMessagesCount());
        assertEquals("Wrong message received", Arrays.asList("first"), aggregator.getLastReceivedMessage());

        // wait for the vortex timeout (6000ms for vortext + 2000ms for aggregator timeout + some extra for a test)
        Thread.sleep(9000);

        // now get the messages which were lagging behind
        assertEquals("Other messages never received by aggregator.", 2, aggregator.getReceivedMessagesCount());
        assertEquals("Wrong message received", Arrays.asList("second"), aggregator.getLastReceivedMessage());
    }
}
