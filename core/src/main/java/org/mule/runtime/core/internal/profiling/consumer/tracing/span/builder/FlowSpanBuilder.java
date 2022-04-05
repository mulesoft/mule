/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.consumer.tracing.span.builder;

import static org.mule.runtime.core.internal.profiling.consumer.tracing.span.builder.FlowSpanIdentifier.flowSpanIdentifierFrom;

import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.api.profiling.tracing.SpanIdentifier;

/**
 * A {@link SpanBuilder} that creates {@link Span} for flows.
 *
 * @since 4.5.0
 */
public class FlowSpanBuilder extends SpanBuilder {

  public static FlowSpanBuilder builder() {
    return new FlowSpanBuilder();
  }

  @Override
  protected Span getParent() {
    return null;
  }

  @Override
  protected SpanIdentifier getSpanIdentifier() {
    return flowSpanIdentifierFrom(artifactId, location.getLocation(), correlationId);
  }

  @Override
  protected String getSpanName() {
    return getSpanIdentifier().getId();
  }
}
