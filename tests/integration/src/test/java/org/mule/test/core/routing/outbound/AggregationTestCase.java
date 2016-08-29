/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.routing.outbound;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;

import org.mule.runtime.core.api.MessageContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.store.ObjectStoreException;
import org.mule.runtime.core.routing.AggregationException;
import org.mule.runtime.core.routing.EventGroup;
import org.mule.runtime.core.routing.SimpleCollectionAggregator;
import org.mule.runtime.core.routing.correlation.CollectionCorrelatorCallback;
import org.mule.runtime.core.routing.correlation.EventCorrelatorCallback;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Test that aggregators preserve message order in synchronous scenarios (MULE-5998)
 */
// TODO: MULE-9303
@Ignore("MULE-9303 Review aggregator sorting using runFlow")
public class AggregationTestCase extends AbstractIntegrationTestCase {

  private static final String PAYLOAD = "Long string that will be broken up into multiple messages";

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/routing/outbound/aggregation-config.xml";
  }

  @Test
  public void testCollectionAggregator() throws Exception {
    MuleClient client = muleContext.getClient();

    flowRunner("SplitterFlow").withPayload(PAYLOAD).asynchronously().run();
    MuleMessage msg = client.request("test://collectionCreated", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(msg);
    assertTrue(msg.getPayload() instanceof List);

    List<byte[]> chunks =
        ((List<MuleMessage>) msg.getPayload()).stream().map(muleMessage -> (byte[]) muleMessage.getPayload()).collect(toList());
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    for (byte[] chunk : chunks) {
      baos.write(chunk);
    }
    String aggregated = baos.toString();
    assertEquals(PAYLOAD, aggregated);
  }

  @Test
  public void testCustomAggregator() throws Exception {
    MuleClient client = muleContext.getClient();
    flowRunner("SplitterFlow2").withPayload(PAYLOAD).asynchronously().run();
    MuleMessage msg = client.request("test://collectionCreated2", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(msg);
    assertNotNull(msg.getPayload());
    assertTrue(msg.getPayload() instanceof List);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    for (Object obj : (List<?>) msg.getPayload()) {
      assertTrue(obj instanceof MuleEvent);
      MuleEvent event = (MuleEvent) obj;
      assertTrue(event.getMessage().getPayload() instanceof byte[]);
      baos.write((byte[]) event.getMessage().getPayload());
    }
    String aggregated = baos.toString();
    assertEquals(PAYLOAD, aggregated);
  }

  public static class Aggregator extends SimpleCollectionAggregator {

    @Override
    protected EventCorrelatorCallback getCorrelatorCallback(MuleContext context) {
      return new MyCollectionCorrelatorCallback(context, persistentStores, storePrefix);
    }
  }

  static class MyCollectionCorrelatorCallback extends CollectionCorrelatorCallback {

    public MyCollectionCorrelatorCallback(MuleContext muleContext, boolean persistentStores, String storePrefix) {
      super(muleContext, storePrefix);
    }

    @Override
    public MuleEvent aggregateEvents(EventGroup events) throws AggregationException {
      List<MuleEvent> eventList = new ArrayList<>();
      Iterator<MuleEvent> iter = null;
      FlowConstruct fc = null;
      MessageContext executionContext = null;
      try {
        iter = events.iterator(true);
      } catch (ObjectStoreException e) {
        throw new AggregationException(events, null, e);
      }
      while (iter.hasNext()) {
        MuleEvent event = iter.next();
        eventList.add(event);
        executionContext = event.getContext();
        // TODO MULE-10302 delegate this to the builder.
        fc = event.getFlowConstruct();
      }

      return MuleEvent.builder(executionContext).message(MuleMessage.builder().payload(eventList).build())
          .exchangePattern(ONE_WAY).flow(fc).build();
    }
  }
}
