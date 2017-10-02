/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.privileged.registry.LegacyRegistryUtils.lookupObject;
import static org.mule.runtime.core.privileged.registry.LegacyRegistryUtils.registerObject;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.event.CoreEvent.Builder;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.privileged.event.DefaultMuleSession;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;

public class EventBenchmark extends AbstractBenchmark {

  public static final String KEY = "key";
  public static final String VALUE = "value";

  private MuleContext muleContext;
  private Flow flow;
  private CoreEvent event;
  private CoreEvent eventWith10VariablesProperties;
  private CoreEvent eventWith50VariablesProperties;
  private CoreEvent eventWith100VariablesProperties;

  @Setup
  public void setup() throws Exception {
    muleContext = createMuleContextWithServices();
    muleContext.start();
    flow = createFlow(muleContext);
    registerObject(muleContext, FLOW_NAME, flow, FlowConstruct.class);
    Message.Builder messageBuilder = Message.builder().value(PAYLOAD);
    CoreEvent.Builder eventBuilder =
        CoreEvent.builder(create(flow, CONNECTOR_LOCATION)).message(messageBuilder.build());
    event = eventBuilder.build();
    eventWith10VariablesProperties = createMuleEventWithFlowVarsAndProperties(10);
    eventWith50VariablesProperties = createMuleEventWithFlowVarsAndProperties(50);
    eventWith100VariablesProperties = createMuleEventWithFlowVarsAndProperties(50);
  }

  @TearDown
  public void teardown() throws MuleException {
    stopIfNeeded(lookupObject(muleContext, SchedulerService.class));
    muleContext.dispose();
  }

  @Benchmark
  public CoreEvent createEvent() {
    return CoreEvent.builder(create(flow, CONNECTOR_LOCATION)).message(of(PAYLOAD)).build();
  }

  @Benchmark
  public CoreEvent copyEvent() {
    return CoreEvent.builder(event).build();
  }

  @Benchmark
  public CoreEvent copyEventWith20VariablesProperties() {
    return CoreEvent.builder(eventWith10VariablesProperties).build();
  }

  @Benchmark
  public CoreEvent copyEventWith100VariablesProperties() {
    return CoreEvent.builder(eventWith100VariablesProperties).build();
  }

  @Benchmark
  public CoreEvent deepCopyEvent() {
    return CoreEvent.builder(event).message(Message.builder(event.getMessage()).build()).build();
  }

  @Benchmark
  public CoreEvent deepCopyEventWith20VariablesProperties() {
    return CoreEvent.builder(eventWith10VariablesProperties)
        .message(Message.builder(eventWith10VariablesProperties.getMessage()).build()).build();
  }

  @Benchmark
  public CoreEvent deepCopyEventWith50VariablesProperties() {
    return CoreEvent.builder(eventWith50VariablesProperties)
        .message(Message.builder(eventWith50VariablesProperties.getMessage()).build()).build();
  }

  @Benchmark
  public CoreEvent deepCopyEventWith100VariablesProperties() {
    return CoreEvent.builder(eventWith100VariablesProperties)
        .message(Message.builder(eventWith100VariablesProperties.getMessage()).build()).build();
  }

  @Benchmark
  public CoreEvent addEventVariable() {
    return CoreEvent.builder(event).addVariable(KEY, VALUE).build();
  }

  @Benchmark
  public CoreEvent addEventVariableEventWith20VariablesProperties() {
    return CoreEvent.builder(eventWith10VariablesProperties).addVariable(KEY, VALUE).build();
  }

  @Benchmark
  public CoreEvent addEventVariableEventWith50VariablesProperties() {
    return CoreEvent.builder(eventWith50VariablesProperties).addVariable(KEY, VALUE).build();
  }

  @Benchmark
  public CoreEvent addEventVariableEventWith100VariablesProperties() {
    return CoreEvent.builder(eventWith100VariablesProperties).addVariable(KEY, VALUE).build();
  }


  @Benchmark
  public CoreEvent copyWith10FlowVarsAnd10PropertiesWrite1OfEach() throws Exception {
    return PrivilegedEvent.builder(eventWith10VariablesProperties)
        .session(new DefaultMuleSession(((PrivilegedEvent) eventWith10VariablesProperties).getSession()))
        .addVariable("newKey", "val")
        .message(InternalMessage.builder(eventWith10VariablesProperties.getMessage()).addInboundProperty("newKey", "val")
            .addOutboundProperty("newKey", "val").build())
        .build();
  }

  @Benchmark
  public CoreEvent copyWith10FlowVarsAnd10PropertiesWrite5OfEach() throws Exception {
    final PrivilegedEvent.Builder eventBuilder = PrivilegedEvent.builder(eventWith50VariablesProperties);
    eventBuilder.session(new DefaultMuleSession(((PrivilegedEvent) eventWith50VariablesProperties).getSession())).build();
    InternalMessage.Builder builder = InternalMessage.builder(eventWith50VariablesProperties.getMessage());
    for (int j = 1; j <= 5; j++) {
      eventBuilder.addVariable("newKey" + j, "val");
      builder.addInboundProperty("newKey", "val").addOutboundProperty("newKey", "val").build();
    }
    return eventBuilder.message(builder.build()).build();
  }

  @Benchmark
  public CoreEvent copyWith50FlowVarsAnd50PropertiesWrite1OfEach() throws Exception {
    return PrivilegedEvent.builder(eventWith50VariablesProperties)
        .session(new DefaultMuleSession(((PrivilegedEvent) eventWith50VariablesProperties).getSession()))
        .addVariable("newKey", "val")
        .message(InternalMessage.builder(eventWith50VariablesProperties.getMessage()).addInboundProperty("newKey", "val")
            .addOutboundProperty("newKey", "val").build())
        .build();
  }

  @Benchmark
  public CoreEvent copyWith100FlowVarsAndPropertiesWrite25OfEach() throws Exception {
    final PrivilegedEvent.Builder eventBuilder = PrivilegedEvent.builder(eventWith100VariablesProperties);
    eventBuilder.session(new DefaultMuleSession(((PrivilegedEvent) eventWith100VariablesProperties).getSession())).build();
    InternalMessage.Builder builder = InternalMessage.builder(eventWith100VariablesProperties.getMessage());
    for (int j = 1; j <= 25; j++) {
      eventBuilder.addVariable("newKey" + j, "val");
      builder.addInboundProperty("newKey", "val").addOutboundProperty("newKey", "val").build();
    }
    return eventBuilder.message(builder.build()).build();
  }

  private CoreEvent createMuleEvent(Message message, int numProperties) {
    final Builder builder;
    try {
      builder = CoreEvent.builder(create(flow, CONNECTOR_LOCATION)).message(message);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    for (int i = 1; i <= numProperties; i++) {
      builder.addVariable("FlOwVaRiAbLeKeY" + i, "val");
    }
    return builder.build();
  }

  private CoreEvent createMuleEventWithFlowVarsAndProperties(int numProperties) {
    InternalMessage.Builder builder = InternalMessage.builder().value(PAYLOAD);
    for (int i = 1; i <= numProperties; i++) {
      builder.addInboundProperty("InBoUnDpRoPeRtYkEy" + i, "val");
    }
    Message message = builder.build();
    CoreEvent event = createMuleEvent(message, numProperties);
    return event;
  }

}
