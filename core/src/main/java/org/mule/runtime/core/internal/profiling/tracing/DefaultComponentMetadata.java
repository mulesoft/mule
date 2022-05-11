/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.tracing;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.profiling.tracing.ComponentMetadata;

import java.util.Optional;

import static java.util.Optional.ofNullable;

/**
 * Immutable implementation of {@link ComponentMetadata}.
 */
public class DefaultComponentMetadata implements ComponentMetadata {

  private final String correlationId;
  private final String artifactId;
  private final String artifactType;
  private final ComponentLocation componentLocation;

  public DefaultComponentMetadata(String correlationId, String artifactId, String artifactType,
                                  ComponentLocation componentLocation) {
    this.correlationId = correlationId;
    this.artifactId = artifactId;
    this.artifactType = artifactType;
    this.componentLocation = componentLocation;
  }

  @Override
  public String getCorrelationId() {
    return correlationId;
  }

  @Override
  public String getArtifactId() {
    return artifactId;
  }

  @Override
  public String getArtifactType() {
    return artifactType;
  }

  @Override
  public Optional<ComponentLocation> getComponentLocation() {
    return ofNullable(componentLocation);
  }
}
