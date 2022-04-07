/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.consumer.tracing.span.builder;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.api.profiling.tracing.SpanIdentifier;

import org.mule.runtime.core.internal.profiling.consumer.tracing.span.DefaultSpanDuration;
import org.mule.runtime.core.internal.profiling.consumer.tracing.span.ExecutionSpan;
import org.mule.runtime.core.internal.profiling.consumer.tracing.span.InternalSpan;
import org.mule.runtime.core.internal.profiling.consumer.tracing.span.SpanManager;

/**
 * This builder creates a {@link Span} from mule core objects.
 *
 * @since 4.5.0
 */
public abstract class SpanBuilder {

  protected String artifactId;
  protected String correlationId;
  protected ComponentLocation location;
  protected SpanManager spanManager;
  private long start;

  public SpanBuilder withSpanManager(SpanManager spanManager) {
    this.spanManager = spanManager;
    return this;
  }

  public SpanBuilder withArtifactId(String artifactId) {
    this.artifactId = artifactId;
    return this;
  }

  public SpanBuilder withCorrelationId(String correlationId) {
    this.correlationId = correlationId;
    return this;
  }

  public SpanBuilder withLocation(ComponentLocation location) {
    this.location = location;
    return this;
  }

  public SpanBuilder withStart(long start) {
    this.start = start;
    return this;
  }

  /**
   * builds the {@link Span}
   *
   * @return the {@link} the span built
   */
  public InternalSpan build() {
    SpanIdentifier spanIdentifier = getSpanIdentifier();
    return spanManager.getSpan(spanIdentifier,
                               id -> new ExecutionSpan(getSpanName(), getSpanIdentifier(), new DefaultSpanDuration(start),
                                                       getParent()));
  }

  protected abstract Span getParent();

  protected abstract SpanIdentifier getSpanIdentifier();

  protected abstract String getSpanName();

}
