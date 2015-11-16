/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.transformers;

import static org.junit.Assert.assertTrue;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.FruitBasket;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.util.concurrent.Latch;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class AutoTransformerTestCase extends FunctionalTestCase
{
    private static Latch latch;

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/transformer/auto-transformer-test-flow.xml";
    }

    @Test
    public void testInboundAutoTransform() throws Exception
    {
        latch = new Latch();
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://in", new FruitBowl(new Apple(), new Banana()), null);

        assertTrue(latch.await(3000, TimeUnit.MILLISECONDS));
    }

    public static class FruitBasketComponent
    {
        public void process(FruitBasket fb)
        {
            assertTrue(fb.hasApple());
            assertTrue(fb.hasBanana());
            latch.countDown();
        }
    }
}
