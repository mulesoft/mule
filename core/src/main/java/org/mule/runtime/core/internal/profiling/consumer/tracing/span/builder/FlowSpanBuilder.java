/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.consumer.tracing.span.builder;

import static org.mule.runtime.core.internal.profiling.consumer.tracing.span.builder.FlowSpanIdentifier.flowSpanIdentifierFrom;

import static java.util.concurrent.TimeUnit.MINUTES;

import static com.github.benmanes.caffeine.cache.Caffeine.newBuilder;

import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.api.profiling.tracing.SpanIdentifier;

import java.util.ArrayList;
import java.util.List;

import com.github.benmanes.caffeine.cache.Cache;

/**
 * A {@link SpanBuilder} that creates {@link Span} for flows.
 *
 * @since 4.5.0
 */
public class FlowSpanBuilder extends SpanBuilder {

  // TODO: a removal listener has to be added to end the span in case it is not closed by the runtime.
  private static Cache<SpanIdentifier, Span> cache = newBuilder().weakValues().expireAfterAccess(60, MINUTES).build();

  public static Span getFlowSpan(SpanIdentifier identifier) {
    return cache.getIfPresent(identifier);
  }

  public static FlowSpanBuilder builder() {
    return new FlowSpanBuilder();
  }

  @Override
  protected List<SpanIdentifier> getLinkedSpans() {
    return new ArrayList<>();
  }

  @Override
  protected Span getParent() {
    return null;
  }

  @Override
  protected SpanIdentifier getSpanIdentifer() {
    return flowSpanIdentifierFrom(artifactId, location.getLocation(), correlationId);
  }

  @Override
  protected String getSpanName() {
    return getSpanIdentifer().getId();
  }
}
