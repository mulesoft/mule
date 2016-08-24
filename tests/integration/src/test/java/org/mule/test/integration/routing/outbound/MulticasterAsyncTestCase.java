/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.outbound;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.mule.functional.functional.FlowAssert;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.tck.testmodels.fruit.Apple;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class MulticasterAsyncTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/routing/outbound/multicaster-async-test-flow.xml";
  }

  @Test
  public void testSplitter() throws Exception {
    Apple apple = new Apple();
    flowRunner("Distributor").withPayload(apple).asynchronously().run();

    List<Apple> results = new ArrayList<>(3);

    MuleClient client = muleContext.getClient();
    MuleMessage result = client.request("test://collector.queue", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(result);
    results.add((Apple) result.getPayload());

    result = client.request("test://collector.queue", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(result);
    results.add((Apple) result.getPayload());

    result = client.request("test://collector.queue", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(result);
    results.add((Apple) result.getPayload());

    assertThat(results.size(), equalTo(3));

    FlowAssert.verify();
  }
}
