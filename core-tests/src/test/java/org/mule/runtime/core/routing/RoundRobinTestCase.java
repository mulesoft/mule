/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;

import org.mule.runtime.core.DefaultMessageContext;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.construct.Flow;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class RoundRobinTestCase extends AbstractMuleContextTestCase {

  private final static int NUMBER_OF_ROUTES = 10;
  private final static int NUMBER_OF_MESSAGES = 10;
  private final AtomicInteger messageNumber = new AtomicInteger(0);

  public RoundRobinTestCase() {
    setStartContext(true);
  }

  @Test
  public void testRoundRobin() throws Exception {
    RoundRobin rr = new RoundRobin();
    rr.setMuleContext(muleContext);
    MuleSession session = getTestSession(null, muleContext);
    List<TestProcessor> routes = new ArrayList<>(NUMBER_OF_ROUTES);
    for (int i = 0; i < NUMBER_OF_ROUTES; i++) {
      routes.add(new TestProcessor());
    }
    rr.setRoutes(new ArrayList<MessageProcessor>(routes));
    List<Thread> threads = new ArrayList<>(NUMBER_OF_ROUTES);
    for (int i = 0; i < NUMBER_OF_ROUTES; i++) {
      threads.add(new Thread(new TestDriver(session, rr, NUMBER_OF_MESSAGES, getTestFlow())));
    }
    for (Thread t : threads) {
      t.start();
    }
    for (Thread t : threads) {
      t.join();
    }
    for (TestProcessor route : routes) {
      assertEquals(NUMBER_OF_MESSAGES, route.getCount());
    }
  }

  @Test
  public void usesFirstRouteOnFirstRequest() throws Exception {
    RoundRobin roundRobin = new RoundRobin();
    roundRobin.setMuleContext(muleContext);
    List<MessageProcessor> routes = new ArrayList<>(2);
    MessageProcessor route1 = mock(MessageProcessor.class, "route1");
    routes.add(route1);
    MessageProcessor route2 = mock(MessageProcessor.class, "route2");
    routes.add(route2);
    roundRobin.setRoutes(new ArrayList<>(routes));

    MuleMessage message = MuleMessage.builder().payload(singletonList(TEST_MESSAGE)).build();

    Flow flow = getTestFlow();
    roundRobin.process(MuleEvent.builder(DefaultMessageContext.create(flow, TEST_CONNECTOR)).message(message)
        .exchangePattern(REQUEST_RESPONSE).flow(flow).build());

    verify(route1).process(any(MuleEvent.class));
    verify(route2, never()).process(any(MuleEvent.class));
  }

  class TestDriver implements Runnable {

    private MessageProcessor target;
    private int numMessages;
    private MuleSession session;
    private FlowConstruct flowConstruct;

    TestDriver(MuleSession session, MessageProcessor target, int numMessages, FlowConstruct flowConstruct) {
      this.target = target;
      this.numMessages = numMessages;
      this.session = session;
      this.flowConstruct = flowConstruct;
    }

    @Override
    public void run() {
      for (int i = 0; i < numMessages; i++) {
        MuleMessage msg = MuleMessage.builder().payload(TEST_MESSAGE + messageNumber.getAndIncrement()).build();
        MuleEvent event = MuleEvent.builder(DefaultMessageContext.create(flowConstruct, TEST_CONNECTOR)).message(msg)
            .exchangePattern(REQUEST_RESPONSE).flow(flowConstruct).session(session).build();
        try {
          target.process(event);
        } catch (MuleException e) {
          // this is expected
        }
      }
    }
  }

  static class TestProcessor implements MessageProcessor {

    private int count;
    private List<Object> payloads = new ArrayList<>();

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException {
      payloads.add(event.getMessage().getPayload());
      count++;
      if (count % 3 == 0) {
        throw new DefaultMuleException("Mule Exception!");
      }
      return null;
    }

    public int getCount() {
      return count;
    }
  }
}
