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

/**
 * Base class to define bootstrap properties
 */
public class AbstractBootstrapProperty {

  protected final BootstrapService service;
  protected final Boolean optional;
  protected final ArtifactType artifactType;

  /**
   * Creates a bootstrap property
   *
   * @param service service that provides the property. Not null.
   * @param artifactType defines what is the artifact this bootstrap object applies to
   * @param optional indicates whether or not the bootstrapped transformer is optional. When a bootstrap object is optional, any
   *        error creating it will be ignored.
   */
  public AbstractBootstrapProperty(BootstrapService service, ArtifactType artifactType, Boolean optional) {
    checkArgument(service != null, "service cannot be null");
    checkArgument(artifactType != null, "artifactType cannot be null");

    this.optional = optional;
    this.artifactType = artifactType;
    this.service = service;
  }

  public BootstrapService getService() {
    return service;
  }

  public Boolean getOptional() {
    return optional;
  }

  public ArtifactType getArtifactType() {
    return artifactType;
  }
}
