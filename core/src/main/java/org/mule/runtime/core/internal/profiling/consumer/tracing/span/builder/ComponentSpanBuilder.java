/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.consumer.tracing.span.builder;

import static org.mule.runtime.core.internal.profiling.consumer.tracing.span.builder.ComponentSpanIdentifier.componentSpanIdentifierFrom;
import static org.mule.runtime.core.internal.profiling.consumer.tracing.span.builder.FlowSpanIdentifier.flowSpanIdentifierFrom;

import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.api.profiling.tracing.SpanIdentifier;

/**
 * A {@link SpanBuilder} that creates {@link Span} for executable components within a mule app flow.
 *
 * @since 4.5.0
 */
public class ComponentSpanBuilder extends SpanBuilder {

  public static ComponentSpanBuilder builder() {
    return new ComponentSpanBuilder();
  }

  @Override
  protected Span getParent() {
    verifyBuilderParameters();

    return spanManager.getSpanIfPresent(getParentIdentifier());
  }

  private FlowSpanIdentifier getParentIdentifier() {

    verifyBuilderParameters();
    return flowSpanIdentifierFrom(artifactId, location.getRootContainerName(), correlationId);
  }


  private void verifyBuilderParameters() {
    if (location == null) {
      throw new IllegalStateException("No location found for the span");
    }

    if (artifactId == null) {
      throw new IllegalStateException("No artifact id found for the span");
    }

    if (correlationId == null) {
      throw new IllegalStateException("No correlationId found for the span");
    }
  }

  @Override
  protected SpanIdentifier getSpanIdentifer() {
    return componentSpanIdentifierFrom(artifactId, location, correlationId);
  }

  @Override
  protected String getSpanName() {
    return componentSpanIdentifierFrom(artifactId, location, correlationId).getId();
  }

}
