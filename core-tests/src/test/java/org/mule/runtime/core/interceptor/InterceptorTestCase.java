/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.interceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.construct.Flow.builder;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.interceptor.Interceptor;
import org.mule.runtime.core.component.AbstractComponent;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.management.stats.ProcessingTime;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;

public class InterceptorTestCase extends AbstractMuleContextTestCase {

  private static final Logger LOGGER = getLogger(InterceptorTestCase.class);

  private final String BEFORE = "Before";
  private final String AFTER = "After";
  private final String COMPONENT = "component";
  private final String INTERCEPTOR_ONE = "inteceptor1";
  private final String INTERCEPTOR_TWO = "inteceptor2";
  private final String INTERCEPTOR_THREE = "inteceptor3";

  private final String SINGLE_INTERCEPTOR_RESULT = INTERCEPTOR_ONE + BEFORE + COMPONENT + INTERCEPTOR_ONE + AFTER;
  private final String MULTIPLE_INTERCEPTOR_RESULT = INTERCEPTOR_ONE + BEFORE + INTERCEPTOR_TWO + BEFORE + INTERCEPTOR_THREE
      + BEFORE + COMPONENT + INTERCEPTOR_THREE + AFTER + INTERCEPTOR_TWO + AFTER + INTERCEPTOR_ONE + AFTER;

  @Test
  public void testSingleInterceptor() throws Exception {
    Flow flow = createUninitializedFlow();
    TestComponent component = (TestComponent) flow.getProcessors().get(0);

    List<Interceptor> interceptors = new ArrayList<>();
    interceptors.add(new TestInterceptor(INTERCEPTOR_ONE));
    component.setInterceptors(interceptors);
    flow.initialise();
    flow.start();

    Event result = component
        .process(Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR_LOCATION)).message(of("")).build());

    assertEquals(SINGLE_INTERCEPTOR_RESULT, result.getMessageAsString(muleContext));
  }

  @Test
  public void testMultipleInterceptor() throws Exception {
    Flow flow = createUninitializedFlow();
    TestComponent component = (TestComponent) flow.getProcessors().get(0);

    List<Interceptor> interceptors = new ArrayList<>();
    interceptors.add(new TestInterceptor(INTERCEPTOR_ONE));
    interceptors.add(new TestInterceptor(INTERCEPTOR_TWO));
    interceptors.add(new TestInterceptor(INTERCEPTOR_THREE));
    component.setInterceptors(interceptors);
    flow.initialise();
    flow.start();

    Event result = component.process(Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR_LOCATION))
        .message(of(""))
        .build());

    assertEquals(MULTIPLE_INTERCEPTOR_RESULT, result.getMessageAsString(muleContext));
  }

  @Test
  public void testSingleInterceptorStack() throws Exception {
    Flow flow = createUninitializedFlow();
    TestComponent component = (TestComponent) flow.getProcessors().get(0);

    List<Interceptor> interceptors = new ArrayList<>();
    List<Interceptor> stackInterceptors = new ArrayList<>();
    stackInterceptors.add(new TestInterceptor(INTERCEPTOR_ONE));
    interceptors.add(new InterceptorStack(stackInterceptors));
    component.setInterceptors(interceptors);
    flow.initialise();
    flow.start();

    Event result = component.process(Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR_LOCATION))
        .message(of(""))
        .build());

    assertEquals(SINGLE_INTERCEPTOR_RESULT, result.getMessageAsString(muleContext));
  }

  @Test
  public void testMultipleInterceptorStack() throws Exception {
    Flow flow = createUninitializedFlow();
    TestComponent component = (TestComponent) flow.getProcessors().get(0);

    List<Interceptor> interceptors = new ArrayList<>();
    interceptors.add(new TestInterceptor(INTERCEPTOR_ONE));
    List<Interceptor> stackInterceptors = new ArrayList<>();
    stackInterceptors.add(new TestInterceptor(INTERCEPTOR_TWO));
    stackInterceptors.add(new TestInterceptor(INTERCEPTOR_THREE));
    interceptors.add(new InterceptorStack(stackInterceptors));
    component.setInterceptors(interceptors);
    flow.initialise();
    flow.start();

    Event result = process(component, Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR_LOCATION))
        .message(of(""))
        .build());

    assertEquals(MULTIPLE_INTERCEPTOR_RESULT, result.getMessageAsString(muleContext));
  }

  @Test
  public void testMultipleInterceptorStack2() throws Exception {
    Flow flow = createUninitializedFlow();
    TestComponent component = (TestComponent) flow.getProcessors().get(0);

    List<Interceptor> interceptors = new ArrayList<>();
    interceptors.add(new TestInterceptor(INTERCEPTOR_ONE));
    interceptors.add(new TestInterceptor(INTERCEPTOR_TWO));
    interceptors.add(new TestInterceptor(INTERCEPTOR_THREE));
    List<Interceptor> stackInterceptors = new ArrayList<>();
    stackInterceptors.add(new TestInterceptor(INTERCEPTOR_ONE));
    stackInterceptors.add(new TestInterceptor(INTERCEPTOR_TWO));
    stackInterceptors.add(new TestInterceptor(INTERCEPTOR_THREE));
    interceptors.add(new InterceptorStack(stackInterceptors));
    component.setInterceptors(interceptors);
    flow.initialise();
    flow.start();

    Event result = process(component, Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR_LOCATION))
        .message(of(""))
        .build());

    assertEquals(INTERCEPTOR_ONE + BEFORE + INTERCEPTOR_TWO + BEFORE + INTERCEPTOR_THREE + BEFORE + INTERCEPTOR_ONE + BEFORE
        + INTERCEPTOR_TWO + BEFORE + INTERCEPTOR_THREE + BEFORE + COMPONENT + INTERCEPTOR_THREE + AFTER + INTERCEPTOR_TWO + AFTER
        + INTERCEPTOR_ONE + AFTER + INTERCEPTOR_THREE + AFTER + INTERCEPTOR_TWO + AFTER + INTERCEPTOR_ONE + AFTER,
                 result.getMessageAsString(muleContext));
  }

  class TestInterceptor extends AbstractEnvelopeInterceptor {

    private String name;

    public TestInterceptor(String name) {
      this.name = name;
    }

    @Override
    public Event after(Event event) {
      try {
        event = Event.builder(event).message(InternalMessage.builder(event.getMessage())
            .payload(getPayloadAsString(event.getMessage()) + name + AFTER).build()).build();
      } catch (Exception e) {
        fail(e.getMessage());
      }
      return event;
    }

    @Override
    public Event before(Event event) {
      try {
        event = Event.builder(event).message(InternalMessage.builder(event.getMessage())
            .payload(getPayloadAsString(event.getMessage()) + name + BEFORE).build()).build();
      } catch (Exception e) {
        fail(e.getMessage());
      }
      return event;
    }

    @Override
    public Event last(Event event, ProcessingTime time, long startTime, boolean exceptionWasThrown) throws MuleException {
      return event;
    }
  }

  private Flow flow;

  protected Flow createUninitializedFlow() throws Exception {
    TestComponent component = new TestComponent();
    flow = builder("name", muleContext).processors(component).build();
    return flow;
  }

  @After
  public void after() throws MuleException {
    stopIfNeeded(flow);
    disposeIfNeeded(flow, LOGGER);
  }

  class TestComponent extends AbstractComponent {

    @Override
    protected Object doInvoke(Event event, Event.Builder eventBuilder) throws Exception {
      return event.getMessageAsString(muleContext) + COMPONENT;
    }
  }
}
