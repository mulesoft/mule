/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.context.notification.RoutingNotificationListener;
import org.mule.runtime.core.context.notification.RoutingNotification;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.hamcrest.core.Is;
import org.junit.Test;

public class CollectionAggregatorRouterTimeoutTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "collection-aggregator-router-timeout-test-flow.xml";
  }

  @Test
  public void testNoFailOnTimeout() throws Exception {
    // correlation timeouts should not fire in this scenario, check it
    final AtomicInteger correlationTimeoutCount = new AtomicInteger(0);
    muleContext.registerListener(new RoutingNotificationListener<RoutingNotification>() {

      @Override
      public void onNotification(RoutingNotification notification) {
        if (notification.getAction() == RoutingNotification.CORRELATION_TIMEOUT) {
          correlationTimeoutCount.incrementAndGet();
        }
      }
    });

    FunctionalTestComponent vortex = (FunctionalTestComponent) getComponent("vortex");
    FunctionalTestComponent aggregator = (FunctionalTestComponent) getComponent("aggregator");

    MuleClient client = muleContext.getClient();
    List<String> list = Arrays.asList("first", "second");

    flowRunner("splitter").withPayload(list).asynchronously().run();

    Thread.sleep(RECEIVE_TIMEOUT);

    // no correlation timeout should ever fire
    assertThat("Correlation timeout should not have happened.", correlationTimeoutCount.intValue(), is(0));

    // should receive only the second message
    assertThat("Vortex received wrong number of messages.", vortex.getReceivedMessagesCount(), is(1));
    assertThat("Wrong message received", vortex.getLastReceivedMessage(), is("second"));

    // should receive only the first part
    assertThat("Aggregator received wrong number of messages.", aggregator.getReceivedMessagesCount(), is(1));
    assertThat("Wrong message received", ((List<MuleMessage>) aggregator.getLastReceivedMessage()).get(0).getPayload(),
               is("first"));

    // wait for the vortex timeout (6000ms for vortext + 2000ms for aggregator
    // timeout + some extra for a test)
    new PollingProber(2 * RECEIVE_TIMEOUT, 200).check(new Probe() {

      @Override
      public boolean isSatisfied() {
        // now get the messages which were lagging behind
        // it will receive only one (first) as second will be discarded by the worker
        // because it has already dispatched one with the same group id
        return aggregator.getReceivedMessagesCount() == 1;
      }

      @Override
      public String describeFailure() {
        return "Other messages never received by aggregator.";
      }
    });

    assertThat(client.request("test://out", RECEIVE_TIMEOUT).getRight().isPresent(), is(true));
  }
}
