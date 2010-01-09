/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.routing.outbound;

import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.Orange;

import java.util.List;

public class ExpressionSplitterSyncTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/outbound/expression-splitter-sync-test.xml";
    }

    public void testRecipientList() throws Exception
    {
        FruitBowl fruitBowl = new FruitBowl(new Apple(), new Banana());
        fruitBowl.addFruit(new Orange());

        MuleClient client = new MuleClient();
        MuleMessage result = client.send("vm://distributor.queue", fruitBowl, null);

        assertNotNull(result);
        assertTrue(result instanceof MuleMessageCollection);
        MuleMessageCollection coll = (MuleMessageCollection) result;
        assertEquals(3, coll.size());
        List<?> results = (List<?>) coll.getPayload();

        assertTrue(results.contains("Apple Received in ServiceOne"));
        assertTrue(results.contains("Banana Received in ServiceTwo"));
        assertTrue(results.contains("Orange Received in ServiceThree"));
    }
}
