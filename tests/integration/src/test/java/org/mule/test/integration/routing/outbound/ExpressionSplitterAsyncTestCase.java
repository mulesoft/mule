/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.Orange;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class ExpressionSplitterAsyncTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/routing/outbound/expression-splitter-async-test-flow.xml";
    }

    @Test
    public void testSplitter() throws Exception
    {
        FruitBowl fruitBowl = new FruitBowl(new Apple(), new Banana());
        fruitBowl.addFruit(new Orange());

        MuleClient client = muleContext.getClient();
        MuleMessage request = new DefaultMuleMessage(fruitBowl, muleContext);

        client.dispatch("vm://distributor.queue", request);

        List<Object> results = new ArrayList<Object>(3);

        MuleMessage result = client.request("vm://collector.queue", 5000);
        assertNotNull(result);
        assertEquals(request.getMessageRootId(), result.getMessageRootId());
        results.add(result.getPayload());

        result = client.request("vm://collector.queue", 3000);
        assertNotNull(result);
        assertEquals(request.getMessageRootId(), result.getMessageRootId());
        results.add(result.getPayload());

        result = client.request("vm://collector.queue", 3000);
        assertNotNull(result);
        assertEquals(request.getMessageRootId(), result.getMessageRootId());
        results.add(result.getPayload());

        assertTrue(results.contains("Apple Received in ServiceOne"));
        assertTrue(results.contains("Banana Received in ServiceTwo"));
        assertTrue(results.contains("Orange Received in ServiceThree"));
    }
}
