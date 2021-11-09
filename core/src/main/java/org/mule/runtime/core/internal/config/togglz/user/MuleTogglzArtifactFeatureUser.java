/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config.togglz.user;

import org.mule.runtime.core.internal.config.togglz.scope.MuleTogglzArtifactFeatureScope;

/**
 * A {@link MuleTogglzFeatureUser} that corresponds to an artifact.
 */
public class MuleTogglzArtifactFeatureUser implements MuleTogglzFeatureUser {

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
