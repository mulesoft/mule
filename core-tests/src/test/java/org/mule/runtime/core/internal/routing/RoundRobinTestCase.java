/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.component.location.ConfigurationComponentLocator.REGISTRY_KEY;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.internal.exception.ErrorTypeRepositoryFactory.createDefaultErrorTypeRepository;
import static org.mule.tck.MuleTestUtils.OBJECT_ERROR_TYPE_REPO_REGISTRY_KEY;
import static org.mule.tck.MuleTestUtils.getTestFlow;
import static org.mule.tck.util.MuleContextUtils.eventBuilder;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.privileged.event.DefaultMuleSession;
import org.mule.runtime.core.privileged.event.MuleSession;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RoundRobinTestCase extends AbstractMuleContextTestCase {

  private final static int NUMBER_OF_ROUTES = 10;
  private final static int NUMBER_OF_MESSAGES = 10;
  private final AtomicInteger messageNumber = new AtomicInteger(0);

  private RoundRobin roundRobin;

  public RoundRobinTestCase() {
    setStartContext(true);
  }

  @Before
  public void before() {
    roundRobin = new RoundRobin();
  }

  @After
  public void after() throws MuleException {
    stopIfNeeded(roundRobin);
    disposeIfNeeded(roundRobin, getLogger(getClass()));
  }

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    when(componentLocator.find(any(Location.class))).thenReturn(empty());
    when(componentLocator.find(any(ComponentIdentifier.class))).thenReturn(emptyList());

    return of(REGISTRY_KEY, componentLocator,
              OBJECT_ERROR_TYPE_REPO_REGISTRY_KEY, createDefaultErrorTypeRepository());
  }

  @Test
  public void testRoundRobin() throws Exception {
    roundRobin.setAnnotations(getAppleFlowComponentLocationAnnotations());
    MuleSession session = new DefaultMuleSession();
    List<TestProcessor> routes = new ArrayList<>(NUMBER_OF_ROUTES);
    for (int i = 0; i < NUMBER_OF_ROUTES; i++) {
      routes.add(new TestProcessor());
    }
    routes.forEach(r -> roundRobin.addRoute(r));
    initialiseIfNeeded(roundRobin, muleContext);
    startIfNeeded(roundRobin);

    List<Thread> threads = new ArrayList<>(NUMBER_OF_ROUTES);
    for (int i = 0; i < NUMBER_OF_ROUTES; i++) {
      threads.add(new Thread(new TestDriver(session, roundRobin, NUMBER_OF_MESSAGES, getTestFlow(muleContext))));
    }
    for (Thread t : threads) {
      t.start();
    }
    for (Thread t : threads) {
      t.join();
    }

    for (TestProcessor route : routes) {
      assertThat(route.getCount(), is(NUMBER_OF_MESSAGES));
    }
  }

  @Test
  public void usesFirstRouteOnFirstRequest() throws Exception {
    roundRobin.setAnnotations(getAppleFlowComponentLocationAnnotations());
    List<Processor> routes = new ArrayList<>(2);
    TestProcessor route1 = new TestProcessor();
    routes.add(route1);
    TestProcessor route2 = new TestProcessor();
    routes.add(route2);
    routes.forEach(r -> roundRobin.addRoute(r));

    initialiseIfNeeded(roundRobin, muleContext);
    startIfNeeded(roundRobin);

    Message message = of(singletonList(TEST_MESSAGE));

    roundRobin.process(eventBuilder(muleContext).message(message).build());

    assertThat(route2.getCount(), is(0));
    assertThat(route1.getCount(), is(1));
  }

  class TestDriver implements Runnable {

    private final Processor target;
    private final int numMessages;
    private final MuleSession session;
    private final FlowConstruct flowConstruct;

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
        CoreEvent event =
            InternalEvent.builder(create(flowConstruct, TEST_CONNECTOR_LOCATION)).message(msg).session(session).build();
        try {
          target.process(event);
        } catch (MuleException e) {
          // this is expected
        }
      }
    }
  }

  static class TestProcessor implements Processor {

    private final AtomicInteger count = new AtomicInteger(0);
    private final List<Object> payloads = new ArrayList<>();

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      payloads.add(event.getMessage().getPayload().getValue());
      if (count.incrementAndGet() % 3 == 0) {
        throw new DefaultMuleException("Mule Exception!");
      }
      return null;
    }

    public int getCount() {
      return count.get();
    }
  }
}
