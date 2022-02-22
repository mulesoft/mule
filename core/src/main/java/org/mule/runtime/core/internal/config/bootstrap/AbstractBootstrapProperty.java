/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.config.bootstrap;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.config.bootstrap.BootstrapService;

import java.util.Set;

/**
 * Base class to define bootstrap properties
 */
public class AbstractBootstrapProperty {

  protected final BootstrapService service;
  protected final Boolean optional;
  protected final Set<ArtifactType> artifactTypes;

  /**
   * Creates a bootstrap property
   *
   * @param service       service that provides the property. Not null.
   * @param artifactTypes defines what is the artifact this bootstrap object applies to
   * @param optional      indicates whether or not the bootstrapped transformer is optional. When a bootstrap object is optional,
   *                      any error creating it will be ignored.
   */
  public AbstractBootstrapProperty(BootstrapService service, Set<ArtifactType> artifactTypes, Boolean optional) {
    checkArgument(service != null, "service cannot be null");
    checkArgument(artifactTypes != null, "artifactTypes cannot be null");
    checkArgument(!artifactTypes.isEmpty(), "artifactTypes cannot be empty");

    this.optional = optional;
    this.artifactTypes = artifactTypes;
    this.service = service;
  }

  public BootstrapService getService() {
    return service;
  }

  // TODO W-10736276 Remove this
  public Boolean getOptional() {
    return optional;
  }

  public Set<ArtifactType> getArtifactTypes() {
    return artifactTypes;
  }
}
