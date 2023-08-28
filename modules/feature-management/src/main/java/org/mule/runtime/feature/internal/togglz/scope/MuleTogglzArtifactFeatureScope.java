/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.feature.internal.togglz.scope;

import org.mule.runtime.feature.internal.togglz.scope.type.MuleTogglzFeatureScopeType;

import java.util.Objects;

import static org.mule.runtime.feature.internal.togglz.scope.type.MuleTogglzFeatureScopeType.ARTIFACT_SCOPE_TYPE;

/**
 * A {@link MuleTogglzFeatureScope} that represents the scope of artifact. When a feature is enabled/disabled using this scope, it
 * will change the status for the corresponding artifact, that is.
 *
 * @since 4.5.0
 */
public class MuleTogglzArtifactFeatureScope implements MuleTogglzFeatureScope {

  private String artifactId;

  public MuleTogglzArtifactFeatureScope(String artifactId) {
    this.artifactId = artifactId;
  }

  public String getArtifactId() {
    return this.artifactId;
  }

  @Override
  public MuleTogglzFeatureScopeType getScopeType() {
    return ARTIFACT_SCOPE_TYPE;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MuleTogglzArtifactFeatureScope that = (MuleTogglzArtifactFeatureScope) o;
    return Objects.equals(artifactId, that.artifactId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(artifactId);
  }
}
