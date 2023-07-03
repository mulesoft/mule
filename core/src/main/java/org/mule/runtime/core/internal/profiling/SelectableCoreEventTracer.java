/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.tracer.api.EventTracer;
import org.mule.runtime.tracer.api.context.getter.DistributedTraceContextGetter;
import org.mule.runtime.tracer.api.sniffer.SpanSnifferManager;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.api.span.validation.Assertion;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A switchable {@link EventTracer} for two wrapped tracers.
 */
public class SelectableCoreEventTracer implements EventTracer<CoreEvent> {

  private final EventTracer<CoreEvent> firstCoreEventTracer;
  private final EventTracer<CoreEvent> secondCoreEventTracer;

  private EventTracer<CoreEvent> currentEventTracer;

  public SelectableCoreEventTracer(EventTracer<CoreEvent> firstCoreEventTracer,
                                   EventTracer<CoreEvent> secondCoreEventTracer,
                                   boolean useFirstCoreEventTracer) {
    this.firstCoreEventTracer = firstCoreEventTracer;
    this.secondCoreEventTracer = secondCoreEventTracer;
    this.currentEventTracer = useFirstCoreEventTracer ? firstCoreEventTracer : secondCoreEventTracer;
  }

  /**
   * if {@parqm useFirstCoreEventTracer} we use the first core event tracer.
   *
   * @param useFirstCoreEventTracer use the first core event tracer.
   */
  public synchronized void useFirstCoreEventTracer(boolean useFirstCoreEventTracer) {
    if (useFirstCoreEventTracer) {
      currentEventTracer = firstCoreEventTracer;
    } else {
      currentEventTracer = secondCoreEventTracer;
    }
  }

  @Override
  public Optional<InternalSpan> startComponentSpan(CoreEvent event, InitialSpanInfo spanInfo) {
    return currentEventTracer.startComponentSpan(event, spanInfo);
  }

  @Override
  public Optional<InternalSpan> startComponentSpan(CoreEvent event, InitialSpanInfo spanInfo, Assertion assertion) {
    return currentEventTracer.startComponentSpan(event, spanInfo, assertion);
  }

  @Override
  public void endCurrentSpan(CoreEvent event) {
    currentEventTracer.endCurrentSpan(event);
  }

  @Override
  public void endCurrentSpan(CoreEvent event, Assertion condition) {
    currentEventTracer.endCurrentSpan(event, condition);
  }

  @Override
  public void injectDistributedTraceContext(EventContext eventContext,
                                            DistributedTraceContextGetter distributedTraceContextGetter) {
    currentEventTracer.injectDistributedTraceContext(eventContext, distributedTraceContextGetter);
  }

  @Override
  public void recordErrorAtCurrentSpan(CoreEvent event, Supplier<Error> errorSupplier, boolean isErrorEscapingCurrentSpan) {
    currentEventTracer.recordErrorAtCurrentSpan(event, errorSupplier, isErrorEscapingCurrentSpan);
  }

  @Override
  public void setCurrentSpanName(CoreEvent event, String name) {
    currentEventTracer.setCurrentSpanName(event, name);
  }

  @Override
  public void addCurrentSpanAttribute(CoreEvent event, String key, String value) {
    currentEventTracer.addCurrentSpanAttribute(event, key, value);
  }

  @Override
  public void addCurrentSpanAttributes(CoreEvent event, Map<String, String> attributes) {
    currentEventTracer.addCurrentSpanAttributes(event, attributes);
  }

  @Override
  public Map<String, String> getDistributedTraceContextMap(CoreEvent event) {
    return currentEventTracer.getDistributedTraceContextMap(event);
  }

  @Override
  public void recordErrorAtCurrentSpan(CoreEvent event, boolean isErrorEscapingCurrentSpan) {
    currentEventTracer.recordErrorAtCurrentSpan(event, isErrorEscapingCurrentSpan);
  }

  @Override
  public SpanSnifferManager getSpanSnifferManager() {
    return currentEventTracer.getSpanSnifferManager();
  }

  public EventTracer<CoreEvent> getCurrentEventTracer() {
    return currentEventTracer;
  }
}
