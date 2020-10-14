/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.maven;

import static java.util.Collections.emptySet;

import org.mule.runtime.module.artifact.api.classloader.ExportedService;

import java.util.Set;

/**
 * Defines the attributes of an artifact. The set of packages and resources.
 */
class ArtifactAttributes {

  private final Set<String> packages;
  private final Set<String> resources;
  private final Set<ExportedService> services;

  public ArtifactAttributes(Set<String> packages, Set<String> resources) {
    this(packages, resources, emptySet());
  }

  public ArtifactAttributes(Set<String> packages, Set<String> resources, Set<ExportedService> services) {
    this.packages = packages;
    this.resources = resources;
    this.services = services;
  }

  public Set<String> getPackages() {
    return packages;
  }

  public Set<String> getResources() {
    return resources;
  }

  public Set<ExportedService> getServices() {
    return services;
  }
}
