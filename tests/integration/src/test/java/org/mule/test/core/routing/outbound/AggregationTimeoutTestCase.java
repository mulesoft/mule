/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.core.routing.outbound;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.junit.Test;

public class AggregationTimeoutTestCase extends AbstractIntegrationTestCase {

  private static final CountDownLatch blockExecution = new CountDownLatch(1);
  public static final String PROCESS_EVENT = "process";
  public static final String BLOCK_EVENT = "block";
  public static final String PROCESSED_EVENT = "processed";

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/routing/outbound/aggregation-timeout-config.xml";
  }

  @Test
  public void timeoutsAggregationWithPersistentStore() throws Exception {
    List<String> inputData = new ArrayList<>();
    inputData.add(PROCESS_EVENT);
    inputData.add(BLOCK_EVENT);

    try {
      MuleClient client = muleContext.getClient();

      // Need to return control to test case as soon as message is sent, and not wait for response.
      flowRunner("main").withPayload(inputData).withExchangePattern(ONE_WAY).dispatchAsync();

      InternalMessage response = client.request("test://testOut", RECEIVE_TIMEOUT).getRight().get();
      assertThat(response.getPayload().getValue(), instanceOf(List.class));

      List<String> payloads =
          ((List<InternalMessage>) response.getPayload().getValue()).stream().map(m -> (String) m.getPayload().getValue())
              .collect(toList());
      assertThat(payloads.size(), equalTo(1));
      assertThat(payloads, hasItem(PROCESSED_EVENT));
    } finally {
      // Release the blocked thread
      blockExecution.countDown();
    }
  }

  public static class BlockExecutionComponent {

    public Object onCall(Object payload) throws Exception {
      if (payload.equals(BLOCK_EVENT)) {
        blockExecution.await();
      }

      return PROCESSED_EVENT;
    }
  }
}
