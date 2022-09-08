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
import static org.mule.runtime.core.internal.profiling.tracing.event.tracer.TracingCondition.NO_CONDITION;
import static org.mule.runtime.core.internal.profiling.tracing.event.tracer.impl.DefaultCoreEventTracerUtils.safeExecute;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpanError;
import org.mule.runtime.core.internal.profiling.tracing.event.tracer.TracingCondition;
import org.mule.runtime.core.internal.profiling.tracing.event.tracer.TracingConditionNotMetException;
import org.mule.runtime.core.internal.trace.DistributedTraceContext;
import org.mule.runtime.core.internal.event.trace.extractor.TraceContextFieldExtractor;
import org.slf4j.Logger;

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

  private static final Logger LOGGER = getLogger(EventDistributedTraceContext.class);

  private final boolean propagationOfExceptionsInTracing;
  private Map<String, String> tracingFields = new HashMap<>();
  private Map<String, String> baggageItems = new HashMap<>();
  private InternalSpan currentSpan;

  public static EventDistributedContextBuilder builder() {
    return new EventDistributedContextBuilder();
  }

  public static DistributedTraceContext emptyDistributedTraceContext() {
    return new EventDistributedTraceContext(new HashMap<>(), new HashMap<>(), false);
  }

  private EventDistributedTraceContext(TraceContextFieldExtractor tracingFieldExtractor,
                                       TraceContextFieldExtractor baggageItemsExtractor,
                                       DistributedTraceContextGetter getter,
                                       boolean propagationOfExceptionsInTracing) {
    tracingFields.putAll(tracingFieldExtractor.extract(getter));
    baggageItems.putAll(baggageItemsExtractor.extract(getter));
    this.propagationOfExceptionsInTracing = propagationOfExceptionsInTracing;
  }

  private EventDistributedTraceContext(Map<String, String> tracingFields,
                                       Map<String, String> baggageItems,
                                       boolean propagationOfExceptionsInTracing) {
    this.tracingFields = tracingFields;
    this.baggageItems = baggageItems;
    this.propagationOfExceptionsInTracing = propagationOfExceptionsInTracing;
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
        new EventDistributedTraceContext(tracingFields, baggageItems, propagationOfExceptionsInTracing);
    eventDistributedTraceContext.setCurrentSpan(currentSpan, NO_CONDITION);
    return eventDistributedTraceContext;
  }

  @Override
  public void endCurrentContextSpan(TracingCondition tracingCondition) {
    safeExecute(() -> tracingCondition.assertOnCurrentSpan(currentSpan), "Error on tracing condition verification: ",
                propagationOfExceptionsInTracing, LOGGER);

    if (currentSpan != null) {
      currentSpan.end();
      currentSpan = resolveParentAsInternalSpan();
    }
  }

  @Override
  public void recordErrorAtCurrentSpan(InternalSpanError error) {
    if (currentSpan != null) {
      currentSpan.addError(error);
    }
  }

  private InternalSpan resolveParentAsInternalSpan() {
    return getAsInternalSpan(currentSpan.getParent());
  }

  @Override
  public void setCurrentSpan(InternalSpan span, TracingCondition tracingCondition) throws TracingConditionNotMetException {
    safeExecute(() -> tracingCondition.assertOnCurrentSpan(currentSpan),
                "Error on tracing condition verification: ", propagationOfExceptionsInTracing, LOGGER);
    this.currentSpan = span;
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
    private boolean propagationOfExceptionsInTracing;

    private EventDistributedContextBuilder() {}

    public EventDistributedContextBuilder withGetter(DistributedTraceContextGetter distributedTraceContextMapGetter) {
      this.distributedTraceContextMapGetter = distributedTraceContextMapGetter;
      return this;
    }


    public EventDistributedContextBuilder withPropagationOfExceptionsInTracing(boolean propagationOfExceptionsInTracing) {
      this.propagationOfExceptionsInTracing = propagationOfExceptionsInTracing;
      return this;
    }

    public DistributedTraceContext build() {
      return new EventDistributedTraceContext(getDefaultTraceContextFieldsExtractor(),
                                              getDefaultBaggageExtractor(),
                                              distributedTraceContextMapGetter,
                                              propagationOfExceptionsInTracing);
    }

  }
}
