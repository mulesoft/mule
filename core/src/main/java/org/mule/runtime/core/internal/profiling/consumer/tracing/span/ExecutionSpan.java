/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.consumer.tracing.span;

import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.api.profiling.tracing.SpanDuration;
import org.mule.runtime.api.profiling.tracing.SpanIdentifier;

import java.util.Optional;

/**
 * A {@link Span} that represents the trace corresponding to the execution of mule flow or component.
 *
 * @since 4.5.0
 */
public class ExecutionSpan implements Span {

  private final String name;
  private final SpanIdentifier identifier;
  private final Span parent;
  private SpanDuration spanDuration;

  public ExecutionSpan(String name, SpanIdentifier identifier, SpanDuration spanDuration,
                       Span parent) {
    this.name = name;
    this.identifier = identifier;
    this.spanDuration = spanDuration;
    this.parent = parent;
  }

  @Override
  public Optional<Span> getParent() {
    return Optional.ofNullable(parent);
  }

  @Override
  public SpanIdentifier getIdentifier() {
    return identifier;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public SpanDuration getDuration() {
    return spanDuration;
  }

  @Override
  public void end(long endTimestamp) {
    spanDuration = new DefaultSpanDuration(spanDuration.getStart(), endTimestamp);
  }

}
