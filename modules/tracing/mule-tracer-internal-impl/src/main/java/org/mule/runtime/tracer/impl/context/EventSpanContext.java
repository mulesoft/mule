/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.context;

import static java.util.Optional.ofNullable;

import static org.mule.runtime.tracer.api.span.InternalSpan.getAsInternalSpan;
import static org.mule.runtime.tracer.impl.span.DeserializedSpan.createDeserializedRootSpan;

import java.util.Optional;

import org.mule.runtime.tracer.api.context.SpanContext;
import org.mule.runtime.tracer.api.context.getter.DistributedTraceContextGetter;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.error.InternalSpanError;
import org.mule.runtime.tracer.api.span.validation.Assertion;
import org.mule.runtime.tracer.api.span.validation.AssertionFailedException;

/**
 * A {@link SpanContext} associated to an event. A {@link org.mule.runtime.core.api.event.CoreEvent} is the component that travels
 * through the execution of a flow. For tracing purposes the {@link org.mule.runtime.api.event.EventContext} has a
 * {@link SpanContext} that has information that may be propagated through runtime boundaries for distributed tracing purposes.
 *
 * @since 4.5.0
 */
public class EventSpanContext implements SpanContext {

  private final boolean propagateTracingExceptions;
  private InternalSpan currentSpan;

  public static EventSpanContextBuilder builder() {
    return new EventSpanContextBuilder();
  }

  private EventSpanContext(InternalSpan currentSpan,
                           boolean propagateTracingExceptions) {
    this.currentSpan = currentSpan;
    this.propagateTracingExceptions = propagateTracingExceptions;
  }

  @Override
  public SpanContext copy() {
    return new EventSpanContext(currentSpan, propagateTracingExceptions);
  }

  @Override
  public void endSpan(Assertion assertion) {
    assertion.assertOnSpan(currentSpan);
    currentSpan.end();
    currentSpan = resolveParentAsInternalSpan();
  }

  @Override
  public void recordErrorAtSpan(InternalSpanError error) {
    currentSpan.addError(error);
  }

  private InternalSpan resolveParentAsInternalSpan() {
    return getAsInternalSpan(currentSpan.getParent());
  }

  @Override
  public void setSpan(InternalSpan span, Assertion assertion) throws AssertionFailedException {
    assertion.assertOnSpan(currentSpan);
    this.currentSpan = span;
  }

  @Override
  public Optional<InternalSpan> getSpan() {
    return ofNullable(currentSpan);
  }

  /**
   * Builder for {@link EventSpanContext}
   *
   * @since 4.5.0
   */
  public static final class EventSpanContextBuilder {

    private DistributedTraceContextGetter distributedTraceContextMapGetter;
    private boolean propagateTracingExceptions;
    private boolean managedChildSpan;

    private EventSpanContextBuilder() {}

    public EventSpanContextBuilder withGetter(DistributedTraceContextGetter distributedTraceContextMapGetter) {
      this.distributedTraceContextMapGetter = distributedTraceContextMapGetter;
      return this;
    }


    public EventSpanContextBuilder withPropagateTracingExceptions(boolean propagateTracingExceptions) {
      this.propagateTracingExceptions = propagateTracingExceptions;
      return this;
    }

    public EventSpanContextBuilder withManagedChildSpan(boolean managedChildSpan) {
      this.managedChildSpan = managedChildSpan;
      return this;
    }

    public EventSpanContext build() {
      return new EventSpanContext(createDeserializedRootSpan(distributedTraceContextMapGetter, managedChildSpan),
                                  propagateTracingExceptions);
    }

  }
}
