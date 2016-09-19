/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.domain;

import org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor;

/**
 * Represents the description of a domain.
 */
public class DomainDescriptor extends DeployableArtifactDescriptor {

  /**
   * Creates a new domain descriptor
   *
   * @param name domain name. Non empty.
   */
  public DomainDescriptor(String name) {
    super(name);
  }
}
