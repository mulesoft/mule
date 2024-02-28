/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.util.collection.SmallMap.of;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.internal.event.EventQuickCopy.quickCopy;
import static org.mule.runtime.core.privileged.registry.LegacyRegistryUtils.lookupObject;
import static org.mule.runtime.core.privileged.registry.LegacyRegistryUtils.registerObject;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.event.CoreEvent.Builder;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.message.InternalMessage;
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
  private CoreEvent eventWith10Variables;
  private CoreEvent eventWith50Variables;
  private CoreEvent eventWith100Variables;

  @Setup
  public void setup() throws Exception {
    muleContext = createMuleContextWithServices();
    muleContext.start();
    flow = createFlow(muleContext);
    registerObject(muleContext, FLOW_NAME, flow);
    Message.Builder messageBuilder = Message.builder().value(PAYLOAD);
    CoreEvent.Builder eventBuilder =
        CoreEvent.builder(create(flow, CONNECTOR_LOCATION)).message(messageBuilder.build());
    event = eventBuilder.build();
    eventWith10Variables = createMuleEventWithFlowVars(10);
    eventWith50Variables = createMuleEventWithFlowVars(50);
    eventWith100Variables = createMuleEventWithFlowVars(50);
  }

  @TearDown
  public void teardown() throws MuleException {
    muleContext.stop();
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
  public CoreEvent copyEventWith20Variables() {
    return CoreEvent.builder(eventWith10Variables).build();
  }

  @Benchmark
  public CoreEvent copyEventWith100Variable() {
    return CoreEvent.builder(eventWith100Variables).build();
  }

  @Benchmark
  public CoreEvent deepCopyEvent() {
    return CoreEvent.builder(event).message(Message.builder(event.getMessage()).build()).build();
  }

  @Benchmark
  public CoreEvent deepCopyEventWith20Variables() {
    return CoreEvent.builder(eventWith10Variables)
        .message(Message.builder(eventWith10Variables.getMessage()).build()).build();
  }

  @Benchmark
  public CoreEvent deepCopyEventWith50Variables() {
    return CoreEvent.builder(eventWith50Variables)
        .message(Message.builder(eventWith50Variables.getMessage()).build()).build();
  }

  @Benchmark
  public CoreEvent deepCopyEventWith100Variables() {
    return CoreEvent.builder(eventWith100Variables)
        .message(Message.builder(eventWith100Variables.getMessage()).build()).build();
  }

  @Benchmark
  public CoreEvent addEventVariable() {
    return CoreEvent.builder(event).addVariable(KEY, VALUE).build();
  }

  @Benchmark
  public CoreEvent addEventVariableEventWith20Variables() {
    return CoreEvent.builder(eventWith10Variables).addVariable(KEY, VALUE).build();
  }

  @Benchmark
  public CoreEvent addEventVariableEventWith50Variables() {
    return CoreEvent.builder(eventWith50Variables).addVariable(KEY, VALUE).build();
  }

  @Benchmark
  public CoreEvent addEventVariableEventWith100Variables() {
    return CoreEvent.builder(eventWith100Variables).addVariable(KEY, VALUE).build();
  }

  @Benchmark
  public CoreEvent copyWith10FlowVarsWrite1OfEach() throws Exception {
    return PrivilegedEvent.builder(eventWith10Variables)
        .addVariable("newKey", "val")
        .message(InternalMessage.builder(eventWith10Variables.getMessage()).build())
        .build();
  }

  @Benchmark
  public CoreEvent copyWith10FlowVarsWrite5OfEach() throws Exception {
    final PrivilegedEvent.Builder eventBuilder = PrivilegedEvent.builder(eventWith50Variables);
    InternalMessage.Builder builder = InternalMessage.builder(eventWith50Variables.getMessage());
    for (int j = 1; j <= 5; j++) {
      eventBuilder.addVariable("newKey" + j, "val");
    }
    return eventBuilder.message(builder.build()).build();
  }

  @Benchmark
  public CoreEvent copyWith50FlowVarsWrite1OfEach() throws Exception {
    return PrivilegedEvent.builder(eventWith50Variables)
        .addVariable("newKey", "val")
        .message(InternalMessage.builder(eventWith50Variables.getMessage()).build())
        .build();
  }

  @Benchmark
  public CoreEvent copyWith100FlowVarsWrite25OfEach() throws Exception {
    final PrivilegedEvent.Builder eventBuilder = PrivilegedEvent.builder(eventWith100Variables);
    InternalMessage.Builder builder = InternalMessage.builder(eventWith100Variables.getMessage());
    for (int j = 1; j <= 25; j++) {
      eventBuilder.addVariable("newKey" + j, "val");
    }
    return eventBuilder.message(builder.build()).build();
  }

  @Benchmark
  public CoreEvent quickCopyInternalParameters() {
    return InternalEvent.builder(quickCopy(quickCopy(event, of("k1", "v1")), of("k2", "v2")))
        .addInternalParameter("k3", "v3")
        .build();
  }

  private CoreEvent createMuleEvent(Message message, int numVariables) {
    final Builder builder;
    try {
      builder = CoreEvent.builder(create(flow, CONNECTOR_LOCATION)).message(message);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    for (int i = 1; i <= numVariables; i++) {
      builder.addVariable("FlOwVaRiAbLeKeY" + i, "val");
    }
    return builder.build();
  }

  private CoreEvent createMuleEventWithFlowVars(int numVariables) {
    InternalMessage.Builder builder = InternalMessage.builder().value(PAYLOAD);
    Message message = builder.build();
    CoreEvent event = createMuleEvent(message, numVariables);
    return event;
  }

}
