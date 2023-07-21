/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.impl.internal.maven;

import java.util.Set;

/**
 * Defines the attributes of an artifact. The set of packages and resources.
 */
class ArtifactAttributes {

  private Set<String> packages;
  private Set<String> resources;

  public ArtifactAttributes(Set<String> packages, Set<String> resources) {
    this.packages = packages;
    this.resources = resources;
  }

  public Set<String> getPackages() {
    return packages;
  }

  public Set<String> getResources() {
    return resources;
  }
}
