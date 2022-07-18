/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.tracing.event.span;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.profiling.tracing.SpanIdentifier;

import java.util.Objects;

/**
 * A {@link SpanIdentifier} for an executable component within a mule app flow.
 *
 * @since 4.5.0
 */
public class ComponentSpanIdentifier implements SpanIdentifier {

  public static final String UNKNOWN = "unknown";
  private final String artifactId;
  private final String location;
  private final String correlationId;

  private ComponentSpanIdentifier(String artifactId, String location, String correlationId) {
    this.artifactId = artifactId;
    this.location = location;
    this.correlationId = correlationId;
  }

  public static SpanIdentifier componentSpanIdentifierFrom(String artifactId, ComponentLocation location,
                                                           String correlationId) {
    return new ComponentSpanIdentifier(artifactId, getLocation(location), correlationId);
  }

  private static String getLocation(ComponentLocation location) {
    if (location == null) {
      return UNKNOWN;
    }
    return location.getLocation();
  }

  @Override
  public String getId() {
    return artifactId + "/" + location + "/" + correlationId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    ComponentSpanIdentifier that = (ComponentSpanIdentifier) o;
    return Objects.equals(artifactId, that.artifactId) && Objects.equals(location, that.location)
        && Objects.equals(correlationId, that.correlationId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(artifactId, location, correlationId);
  }
}
