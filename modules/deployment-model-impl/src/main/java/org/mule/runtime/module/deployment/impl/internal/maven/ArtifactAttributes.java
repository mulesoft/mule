/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.maven;

import java.util.Set;

public class ArtifactAttributes {

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
