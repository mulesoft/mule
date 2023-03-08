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

import static java.util.Optional.empty;

/**
 * A noop {@link NoopCoreEventTracer}
 *
 * @since 4.5.0
 */
public class NoopCoreEventTracer implements EventTracer<CoreEvent> {

  public static EventTracer<CoreEvent> getNoopCoreEventTracer() {
    return new NoopCoreEventTracer();
  }

  private NoopCoreEventTracer() {}

  @Override
  public Optional<InternalSpan> startComponentSpan(CoreEvent event, InitialSpanInfo initialSpanInfo) {
    return empty();
  }

  @Override
  public Optional<InternalSpan> startComponentSpan(CoreEvent event, InitialSpanInfo initialSpanInfo, Assertion assertion) {
    return empty();
  }

  @Override
  public void endCurrentSpan(CoreEvent event) {
    // Noop
  }

  @Override
  public void endCurrentSpan(CoreEvent event, Assertion condition) {
    // Noop
  }

  @Override
  public void injectDistributedTraceContext(EventContext eventContext,
                                            DistributedTraceContextGetter distributedTraceContextGetter) {
    // Noop
  }

  @Override
  public void recordErrorAtCurrentSpan(CoreEvent event, Supplier<Error> errorSupplier, boolean isErrorEscapingCurrentSpan) {
    // Noop
  }

  @Override
  public void setCurrentSpanName(CoreEvent event, String name) {
    // Noop
  }

  @Override
  public void addCurrentSpanAttribute(CoreEvent event, String key, String value) {
    // Noop
  }

  @Override
  public void addCurrentSpanAttributes(CoreEvent event, Map<String, String> attributes) {
    // Noop
  }

  @Override
  public SpanSnifferManager getSpanSnifferManager() {
    return new NoopSpanSnifferManager();
  }

  private class NoopSpanSnifferManager implements SpanSnifferManager {

  }
}
