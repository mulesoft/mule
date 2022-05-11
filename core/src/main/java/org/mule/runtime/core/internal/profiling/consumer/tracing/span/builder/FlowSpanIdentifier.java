/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.consumer.tracing.span.builder;

import static java.util.Objects.hash;

import org.mule.runtime.api.profiling.tracing.SpanIdentifier;

import java.util.Objects;

/**
 * A {@link SpanIdentifier} for a flow.
 *
 * @since 4.5.0
 */
public class FlowSpanIdentifier implements SpanIdentifier {

  private final String artifactId;
  private final String flowName;
  private final String correlationId;

  public static FlowSpanIdentifier flowSpanIdentifierFrom(String artifactId, String flowName, String correlationId) {
    return new FlowSpanIdentifier(artifactId, flowName, correlationId);
  }

  private FlowSpanIdentifier(String artifactId, String flowName, String correlationId) {
    this.artifactId = artifactId;
    this.flowName = flowName;
    this.correlationId = correlationId;
  }

  @Override
  public String getId() {
    return artifactId + "/" + flowName + "/" + correlationId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    FlowSpanIdentifier that = (FlowSpanIdentifier) o;
    return Objects.equals(artifactId, that.artifactId) && Objects.equals(flowName, that.flowName)
        && Objects.equals(correlationId, that.correlationId);
  }

  @Override
  public int hashCode() {
    return hash(artifactId, flowName, correlationId);
  }
}
