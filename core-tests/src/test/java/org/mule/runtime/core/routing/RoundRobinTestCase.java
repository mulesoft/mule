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
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.tck.MuleTestUtils.getTestFlow;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.session.DefaultMuleSession;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.reactivestreams.Publisher;

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
    MuleSession session = new DefaultMuleSession();
    List<TestProcessor> routes = new ArrayList<>(NUMBER_OF_ROUTES);
    for (int i = 0; i < NUMBER_OF_ROUTES; i++) {
      routes.add(new TestProcessor());
    }
    rr.setRoutes(new ArrayList<Processor>(routes));
    List<Thread> threads = new ArrayList<>(NUMBER_OF_ROUTES);
    for (int i = 0; i < NUMBER_OF_ROUTES; i++) {
      threads.add(new Thread(new TestDriver(session, rr, NUMBER_OF_MESSAGES, getTestFlow(muleContext))));
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
    List<Processor> routes = new ArrayList<>(2);
    Processor route1 = mock(Processor.class, "route1");
    when(route1.apply(any(Publisher.class))).then(invocation -> invocation.getArguments()[0]);
    routes.add(route1);
    Processor route2 = mock(Processor.class, "route2");
    when(route2.apply(any(Publisher.class))).then(invocation -> invocation.getArguments()[0]);
    routes.add(route2);
    roundRobin.setRoutes(new ArrayList<>(routes));

    Message message = of(singletonList(TEST_MESSAGE));

    roundRobin.process(eventBuilder().message(message).build());

    verify(route1).apply(any(Publisher.class));
    verify(route2, never()).apply(any(Publisher.class));
  }

  class TestDriver implements Runnable {

    private Processor target;
    private int numMessages;
    private MuleSession session;
    private FlowConstruct flowConstruct;

    TestDriver(MuleSession session, Processor target, int numMessages, FlowConstruct flowConstruct) {
      this.target = target;
      this.numMessages = numMessages;
      this.session = session;
      this.flowConstruct = flowConstruct;
    }

    @Override
    public void run() {
      for (int i = 0; i < numMessages; i++) {
        Message msg = of(TEST_MESSAGE + messageNumber.getAndIncrement());
        Event event = Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR_LOCATION)).message(msg)
            .flow(flowConstruct).session(session).build();
        try {
          target.process(event);
        } catch (MuleException e) {
          // this is expected
        }
      }
    }
  }

  static class TestProcessor implements Processor {

    private int count;
    private List<Object> payloads = new ArrayList<>();

    @Override
    public Event process(Event event) throws MuleException {
      payloads.add(event.getMessage().getPayload().getValue());
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
