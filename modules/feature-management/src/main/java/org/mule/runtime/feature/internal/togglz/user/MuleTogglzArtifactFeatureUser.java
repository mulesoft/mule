/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.feature.internal.togglz.user;

import org.mule.runtime.feature.internal.togglz.scope.MuleTogglzArtifactFeatureScope;
import org.togglz.core.user.FeatureUser;

/**
 * A {@link MuleFeatureUser} that corresponds to an artifact.
 *
 * @since 4.5.0
 */
public class MuleTogglzArtifactFeatureUser implements MuleFeatureUser, FeatureUser {

  private final String name;
  private final MuleTogglzArtifactFeatureScope scope;

  public MuleTogglzArtifactFeatureUser(String artifactId) {
    this.name = artifactId;
    this.scope = new MuleTogglzArtifactFeatureScope(artifactId);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean isFeatureAdmin() {
    return false;
  }

  @Override
  public Object getAttribute(String name) {
    if (name.equals(FEATURE_SCOPE_ATTRIBUTE_KEY)) {
      return scope;
    }

    return null;
  }
}
