/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.outbound;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.Orange;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class MulticastAsyncWithTransformersTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/routing/outbound/multicaster-async-with-transformers-test-flow.xml";
  }

  @Test
  public void testSyncMulticast() throws Exception {
    Apple apple = new Apple();
    Banana banana = new Banana();
    Orange orange = new Orange();
    FruitBowl fruitBowl = new FruitBowl(apple, banana);
    fruitBowl.addFruit(orange);

    flowRunner("Distributor").withPayload(fruitBowl).asynchronously().run();

    List<Object> results = new ArrayList<Object>(3);

    MuleClient client = muleContext.getClient();
    // We have to wait a lot longer here since groovy takes an age to compile the first time
    MuleMessage result = client.request("test://collector.queue", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(result);
    results.add(result.getPayload());

    result = client.request("test://collector.queue", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(result);
    results.add(result.getPayload());

    result = client.request("test://collector.queue", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(result);
    results.add(result.getPayload());

    assertTrue(results.contains(apple));
    assertTrue(results.contains(banana));
    assertTrue(results.contains(orange));
  }
}
