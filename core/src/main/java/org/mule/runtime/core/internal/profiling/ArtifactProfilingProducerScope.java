/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling;

import org.mule.runtime.api.profiling.ProfilingProducerScopeType;
import org.mule.runtime.api.profiling.ProfilingProducerScope;

import java.util.Objects;

import static org.mule.runtime.api.profiling.ProfilingProducerScopeType.ARTIFACT_SCOPE_TYPE;

/**
 * A {@link ProfilingProducerScope} that let us retrieve {@link org.mule.runtime.api.profiling.ProfilingDataProducer} associated
 * to an artifact.
 *
 * @since 4.5.0
 */
public class ArtifactProfilingProducerScope implements ProfilingProducerScope {

  private final String artifactIdentifier;

  public ArtifactProfilingProducerScope(String artifactIdentifier) {
    this.artifactIdentifier = artifactIdentifier;
  }

  @Override
  public String getProducerScopeIdentifier() {
    return artifactIdentifier;
  }

  @Override
  public ProfilingProducerScopeType getProducerScopeTypeIdentifier() {
    return ARTIFACT_SCOPE_TYPE;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    ArtifactProfilingProducerScope that = (ArtifactProfilingProducerScope) o;
    return Objects.equals(artifactIdentifier, that.artifactIdentifier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(artifactIdentifier);
  }
}
