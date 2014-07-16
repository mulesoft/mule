/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.client.MuleClient;
import org.mule.api.context.notification.RoutingNotificationListener;
import org.mule.context.notification.RoutingNotification;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.functional.FunctionalTestComponent;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class CollectionAggregatorRouterTimeoutTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "collection-aggregator-router-timeout-test-service.xml"},
            {ConfigVariant.FLOW, "collection-aggregator-router-timeout-test-flow.xml"}});
    }

    public CollectionAggregatorRouterTimeoutTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testNoFailOnTimeout() throws Exception
    {
        // correlation timeouts should not fire in this scenario, check it
        final AtomicInteger correlationTimeoutCount = new AtomicInteger(0);
        muleContext.registerListener(new RoutingNotificationListener<RoutingNotification>()
        {
            @Override
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

        MuleClient client = muleContext.getClient();
        List<String> list = Arrays.asList("first", "second");
        client.dispatch("vm://splitter", list, null);

        Thread.sleep(5000);

        // no correlation timeout should ever fire
        assertEquals("Correlation timeout should not have happened.", 0, correlationTimeoutCount.intValue());

        // should receive only the second message
        assertEquals("Vortex received wrong number of messages.", 1, vortex.getReceivedMessagesCount());
        assertEquals("Wrong message received", "second", vortex.getLastReceivedMessage());

        // should receive only the first part
        assertEquals("Aggregator received wrong number of messages.", 1,
            aggregator.getReceivedMessagesCount());
        assertEquals("Wrong message received", Arrays.asList("first"), aggregator.getLastReceivedMessage());

        // wait for the vortex timeout (6000ms for vortext + 2000ms for aggregator
        // timeout + some extra for a test)
        Thread.sleep(9000);

        // now get the messages which were lagging behind
        // it will receive only one (first) as second will be discarded by the worker
        // because it has already dispatched one with the same group id
        assertEquals("Other messages never received by aggregator.", 1, aggregator.getReceivedMessagesCount());
        assertNotNull(client.request("vm://out?connector=queue", 10000));
    }
}
