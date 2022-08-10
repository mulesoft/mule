/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.event.trace;

import static org.mule.runtime.core.internal.event.trace.extractor.RuntimeEventTraceExtractors.getDefaultBaggageExtractor;
import static org.mule.runtime.core.internal.event.trace.extractor.RuntimeEventTraceExtractors.getDefaultTraceContextFieldsExtractor;
import static org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan.getAsInternalSpan;

import static java.util.Optional.ofNullable;

import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.internal.trace.DistributedTraceContext;
import org.mule.runtime.core.internal.event.trace.extractor.TraceContextFieldExtractor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A {@link DistributedTraceContext} associated to an event.
 *
 * A {@link org.mule.runtime.core.api.event.CoreEvent} is the component that travels through the execution of a flow. For tracing
 * purposes the {@link org.mule.runtime.api.event.EventContext} has a {@link DistributedTraceContext} that has information that
 * may be propagated through runtime boundaries for distributed tracing purposes.
 *
 * @since 4.5.0
 */
public class EventDistributedTraceContext implements DistributedTraceContext {

  private Map<String, String> tracingFields = new HashMap<>();
  private Map<String, String> baggageItems = new HashMap<>();
  private InternalSpan currentSpan;

  public static EventDistributedContextBuilder builder() {
    return new EventDistributedContextBuilder();
  }

  public static DistributedTraceContext emptyDistributedTraceContext() {
    return new EventDistributedTraceContext(new HashMap<>(), new HashMap<>());
  }

  private EventDistributedTraceContext(TraceContextFieldExtractor tracingFieldExtractor,
                                       TraceContextFieldExtractor baggageItemsExtractor,
                                       DistributedTraceContextGetter getter) {
    tracingFields.putAll(tracingFieldExtractor.extract(getter));
    baggageItems.putAll(baggageItemsExtractor.extract(getter));
  }

  private EventDistributedTraceContext(Map<String, String> tracingFields,
                                       Map<String, String> baggageItems) {
    this.tracingFields = tracingFields;
    this.baggageItems = baggageItems;
  }

  @Override
  public Optional<String> getTraceFieldValue(String key) {
    return ofNullable(tracingFields.get(key));
  }

  @Override
  public Map<String, String> tracingFieldsAsMap() {
    return tracingFields;
  }

  @Override
  public Optional<String> getBaggageItem(String key) {
    return ofNullable(baggageItems.get(key));
  }

  @Override
  public Map<String, String> baggageItemsAsMap() {
    return baggageItems;
  }

  @Override
  public DistributedTraceContext copy() {
    EventDistributedTraceContext eventDistributedTraceContext =
        new EventDistributedTraceContext(tracingFields, baggageItems);
    eventDistributedTraceContext.setCurrentSpan(currentSpan);
    return eventDistributedTraceContext;
  }

  @Override
  public void endCurrentContextSpan() {
    if (currentSpan != null) {
      currentSpan.end();
      currentSpan = resolveParentAsInternalSpan();
    }
  }

  private InternalSpan resolveParentAsInternalSpan() {
    return getAsInternalSpan(currentSpan.getParent());
  }

  @Override
  public void setCurrentSpan(InternalSpan currentSpan) {
    this.currentSpan = currentSpan;
  }

  @Override
  public Optional<InternalSpan> getCurrentSpan() {
    return ofNullable(currentSpan);
  }

  /**
   * Builder for {@link EventDistributedTraceContext}
   *
   * @since 4.5.0
   */
  public static final class EventDistributedContextBuilder {

    private DistributedTraceContextGetter distributedTraceContextMapGetter;

    private EventDistributedContextBuilder() {}

    public EventDistributedContextBuilder withGetter(DistributedTraceContextGetter distributedTraceContextMapGetter) {
      this.distributedTraceContextMapGetter = distributedTraceContextMapGetter;
      return this;
    }

    public DistributedTraceContext build() {
      return new EventDistributedTraceContext(getDefaultTraceContextFieldsExtractor(),
                                              getDefaultBaggageExtractor(),
                                              distributedTraceContextMapGetter);
    }
  }
}
