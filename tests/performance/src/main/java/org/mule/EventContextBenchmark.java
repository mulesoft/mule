/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.api.exception.NullExceptionHandler.getInstance;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.internal.event.DefaultEventContext.child;
import static org.mule.runtime.core.privileged.registry.LegacyRegistryUtils.lookupObject;
import static org.mule.runtime.core.privileged.registry.LegacyRegistryUtils.registerObject;
import static reactor.core.publisher.Mono.from;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.UUID;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Warmup(iterations = 20)
@Measurement(iterations = 100)
@OutputTimeUnit(NANOSECONDS)
public class EventContextBenchmark extends AbstractBenchmark {

  private MuleContext muleContext;
  private Flow flow;
  private String id = UUID.getUUID();
  private String severId = UUID.getUUID();
  private CoreEvent event;


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
  }

  @TearDown
  public void teardown() throws MuleException {
    stopIfNeeded(lookupObject(muleContext, SchedulerService.class));
    muleContext.dispose();
  }

  @Benchmark
  public EventContext createEventContext() {
    return create(id, severId, CONNECTOR_LOCATION, getInstance());
  }

  @Benchmark
  public EventContext createEventContextWithFlow() {
    return create(flow, CONNECTOR_LOCATION);
  }

  @Benchmark
  public Object[] createEventContextWithFlowAndComplete() {
    AtomicReference<CoreEvent> result = new AtomicReference<>();
    AtomicBoolean complete = new AtomicBoolean();
    BaseEventContext eventContext = (BaseEventContext) create(flow, CONNECTOR_LOCATION);
    from(from(eventContext.getResponsePublisher())).doOnSuccess(response -> result.set(response)).subscribe();
    eventContext.onTerminated((response, throwable) -> complete.set(true));
    eventContext.success(event);
    return new Object[] {result, complete};
  }

  @Benchmark
  public Object[] createEventContextWithFlowAndCompleteWithExternalCompletion() {
    CompletableFuture<Void> completableFuture = new CompletableFuture<>();
    AtomicReference<CoreEvent> result = new AtomicReference<>();
    AtomicBoolean complete = new AtomicBoolean();
    BaseEventContext eventContext = (BaseEventContext) create(flow, CONNECTOR_LOCATION, null, of(completableFuture));
    from(from(eventContext.getResponsePublisher())).doOnSuccess(response -> result.set(response)).subscribe();
    eventContext.onTerminated((response, throwable) -> complete.set(true));
    eventContext.success(event);
    completableFuture.complete(null);
    return new Object[] {result, complete};
  }

  @Benchmark
  public Object[] createEventContextWith10ChildrenTerminateAllAtOnce() {
    return createEventContextTerminateAllAtOnce(10);
  }

  @Benchmark
  public Object[] createEventContextWith1000ChildrenTerminateAllAtOnce() {
    return createEventContextTerminateAllAtOnce(1000);
  }

  private Object[] createEventContextTerminateAllAtOnce(int childrenCount) {
    AtomicReference<CoreEvent> result = new AtomicReference<>();
    AtomicBoolean complete = new AtomicBoolean();
    BaseEventContext eventContext = (BaseEventContext) create(flow, CONNECTOR_LOCATION);

    List<BaseEventContext> children = new ArrayList<>(childrenCount);
    for (int i = 0; i < childrenCount; ++i) {
      children.add(child(eventContext, empty()));
    }

    from(from(eventContext.getResponsePublisher())).doOnSuccess(response -> result.set(response)).subscribe();
    eventContext.onTerminated((response, throwable) -> complete.set(true));
    eventContext.success(event);

    for (BaseEventContext child : children) {
      child.success();
    }

    return new Object[] {result, complete};
  }

  @Benchmark
  public Object[] createEventContextWith10ChildrenForEach() {
    return childEventContextForEach(10);
  }

  @Benchmark
  public Object[] createEventContextWith1000ChildrenForEach() {
    return childEventContextForEach(1000);
  }

  private Object[] childEventContextForEach(int childrenCount) {
    AtomicReference<CoreEvent> result = new AtomicReference<>();
    AtomicBoolean complete = new AtomicBoolean();
    BaseEventContext eventContext = (BaseEventContext) create(flow, CONNECTOR_LOCATION);

    for (int i = 0; i < childrenCount; ++i) {
      BaseEventContext child = child(eventContext, empty());
      child.success();
    }

    from(from(eventContext.getResponsePublisher())).doOnSuccess(response -> result.set(response)).subscribe();
    eventContext.onTerminated((response, throwable) -> complete.set(true));
    eventContext.success(event);
    return new Object[] {result, complete};
  }

}
