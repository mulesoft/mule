/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config.bootstrap;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import static java.util.Objects.requireNonNull;

import org.mule.runtime.api.artifact.ArtifactType;
import org.mule.runtime.core.api.config.bootstrap.BootstrapService;

import java.util.Set;

/**
 * Base class to define bootstrap properties
 */
public class AbstractBootstrapProperty {

  protected final BootstrapService service;
  protected final Set<ArtifactType> artifactTypes;

  /**
   * Creates a bootstrap property
   *
   * @param service       service that provides the property. Not null.
   * @param artifactTypes defines what is the artifact this bootstrap object applies to
   */
  public AbstractBootstrapProperty(BootstrapService service, Set<ArtifactType> artifactTypes) {
    requireNonNull(service, "service cannot be null");
    requireNonNull(artifactTypes, "artifactTypes cannot be null");
    checkArgument(!artifactTypes.isEmpty(), "artifactTypes cannot be empty");

    this.artifactTypes = artifactTypes;
    this.service = service;
  }

  public BootstrapService getService() {
    return service;
  }

  public Set<ArtifactType> getArtifactTypes() {
    return artifactTypes;
  }
}
