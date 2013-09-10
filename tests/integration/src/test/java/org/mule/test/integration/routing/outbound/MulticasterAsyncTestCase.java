/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.outbound;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class MulticasterAsyncTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/integration/routing/outbound/multicaster-async-test-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/routing/outbound/multicaster-async-test-flow.xml"}
        });
    }

    public MulticasterAsyncTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testSplitter() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://distributor.queue", new Apple(), null);

        List<Object> results = new ArrayList<Object>(3);

        MuleMessage result = client.request("vm://collector.queue", 5000);
        assertNotNull(result);
        results.add(result.getPayload());

        result = client.request("vm://collector.queue", 3000);
        assertNotNull(result);
        results.add(result.getPayload());

        result = client.request("vm://collector.queue", 3000);
        assertNotNull(result);
        results.add(result.getPayload());

        assertTrue(results.contains("Apple Received in ServiceOne"));
        assertTrue(results.contains("Apple Received in ServiceTwo"));
        assertTrue(results.contains("Apple Received in ServiceThree"));
    }
}
