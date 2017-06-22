/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.Event.Builder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.api.session.DefaultMuleSession;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;

public class EventBenchmark extends AbstractBenchmark {

  public static final String KEY = "key";
  public static final String VALUE = "value";

  private MuleContext muleContext;
  private Flow flow;
  private Event event;
  private Event eventWith10VariablesProperties;
  private Event eventWith50VariablesProperties;
  private Event eventWith100VariablesProperties;

  @Setup
  public void setup() throws Exception {
    muleContext = createMuleContextWithServices();
    muleContext.start();
    flow = createFlow(muleContext);
    muleContext.getRegistry().registerFlowConstruct(flow);
    Message.Builder messageBuilder = Message.builder().payload(PAYLOAD);
    Event.Builder eventBuilder =
        Event.builder(DefaultEventContext.create(flow, CONNECTOR_LOCATION)).message(messageBuilder.build());
    event = eventBuilder.build();
    eventWith10VariablesProperties = createMuleEventWithFlowVarsAndProperties(10);
    eventWith50VariablesProperties = createMuleEventWithFlowVarsAndProperties(50);
    eventWith100VariablesProperties = createMuleEventWithFlowVarsAndProperties(50);
  }

  @TearDown
  public void teardown() throws MuleException {
    stopIfNeeded(muleContext.getRegistry().lookupObject(SchedulerService.class));
    muleContext.dispose();
  }

  @Benchmark
  public Event createEvent() {
    return Event.builder(DefaultEventContext.create(flow, CONNECTOR_LOCATION)).message(of(PAYLOAD)).build();
  }

  @Benchmark
  public Event copyEvent() {
    return Event.builder(event).build();
  }

  @Benchmark
  public Event copyEventWith20VariablesProperties() {
    return Event.builder(eventWith10VariablesProperties).build();
  }

  @Benchmark
  public Event copyEventWith100VariablesProperties() {
    return Event.builder(eventWith100VariablesProperties).build();
  }

  @Benchmark
  public Event deepCopyEvent() {
    return Event.builder(event).message(Message.builder(event.getMessage()).build()).build();
  }

  @Benchmark
  public Event deepCopyEventWith20VariablesProperties() {
    return Event.builder(eventWith10VariablesProperties)
        .message(Message.builder(eventWith10VariablesProperties.getMessage()).build()).build();
  }

  @Benchmark
  public Event deepCopyEventWith50VariablesProperties() {
    return Event.builder(eventWith50VariablesProperties)
        .message(Message.builder(eventWith50VariablesProperties.getMessage()).build()).build();
  }

  @Benchmark
  public Event deepCopyEventWith100VariablesProperties() {
    return Event.builder(eventWith100VariablesProperties)
        .message(Message.builder(eventWith100VariablesProperties.getMessage()).build()).build();
  }

  @Benchmark
  public Event addEventVariable() {
    return Event.builder(event).addVariable(KEY, VALUE).build();
  }

  @Benchmark
  public Event addEventVariableEventWith20VariablesProperties() {
    return Event.builder(eventWith10VariablesProperties).addVariable(KEY, VALUE).build();
  }

  @Benchmark
  public Event addEventVariableEventWith50VariablesProperties() {
    return Event.builder(eventWith50VariablesProperties).addVariable(KEY, VALUE).build();
  }

  @Benchmark
  public Event addEventVariableEventWith100VariablesProperties() {
    return Event.builder(eventWith100VariablesProperties).addVariable(KEY, VALUE).build();
  }


  @Benchmark
  public Event copyWith10FlowVarsAnd10PropertiesWrite1OfEach() throws Exception {
    return Event.builder(eventWith10VariablesProperties)
        .session(new DefaultMuleSession(eventWith10VariablesProperties.getSession()))
        .addVariable("newKey", "val")
        .message(InternalMessage.builder(eventWith10VariablesProperties.getMessage()).addInboundProperty("newKey", "val")
            .addOutboundProperty("newKey", "val").build())
        .build();
  }

  @Benchmark
  public Event copyWith10FlowVarsAnd10PropertiesWrite5OfEach() throws Exception {
    final Builder eventBuilder = Event.builder(eventWith50VariablesProperties);
    eventBuilder.session(new DefaultMuleSession(eventWith50VariablesProperties.getSession())).build();
    InternalMessage.Builder builder = InternalMessage.builder(eventWith50VariablesProperties.getMessage());
    for (int j = 1; j <= 5; j++) {
      eventBuilder.addVariable("newKey" + j, "val");
      builder.addInboundProperty("newKey", "val").addOutboundProperty("newKey", "val").build();
    }
    return eventBuilder.message(builder.build()).build();
  }

  @Benchmark
  public Event copyWith50FlowVarsAnd50PropertiesWrite1OfEach() throws Exception {
    return Event.builder(eventWith50VariablesProperties)
        .session(new DefaultMuleSession(eventWith50VariablesProperties.getSession()))
        .addVariable("newKey", "val")
        .message(InternalMessage.builder(eventWith50VariablesProperties.getMessage()).addInboundProperty("newKey", "val")
            .addOutboundProperty("newKey", "val").build())
        .build();
  }

  @Benchmark
  public Event copyWith100FlowVarsAndPropertiesWrite25OfEach() throws Exception {
    final Builder eventBuilder = Event.builder(eventWith100VariablesProperties);
    eventBuilder.session(new DefaultMuleSession(eventWith100VariablesProperties.getSession())).build();
    InternalMessage.Builder builder = InternalMessage.builder(eventWith100VariablesProperties.getMessage());
    for (int j = 1; j <= 25; j++) {
      eventBuilder.addVariable("newKey" + j, "val");
      builder.addInboundProperty("newKey", "val").addOutboundProperty("newKey", "val").build();
    }
    return eventBuilder.message(builder.build()).build();
  }

  private Event createMuleEvent(Message message, int numProperties) {
    final Builder builder;
    try {
      builder = Event.builder(DefaultEventContext.create(flow, CONNECTOR_LOCATION)).message(message);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    for (int i = 1; i <= numProperties; i++) {
      builder.addVariable("FlOwVaRiAbLeKeY" + i, "val");
    }
    return builder.build();
  }

  private Event createMuleEventWithFlowVarsAndProperties(int numProperties) {
    InternalMessage.Builder builder = InternalMessage.builder().payload(PAYLOAD);
    for (int i = 1; i <= numProperties; i++) {
      builder.addInboundProperty("InBoUnDpRoPeRtYkEy" + i, "val");
    }
    Message message = builder.build();
    Event event = createMuleEvent(message, numProperties);
    return event;
  }

}
