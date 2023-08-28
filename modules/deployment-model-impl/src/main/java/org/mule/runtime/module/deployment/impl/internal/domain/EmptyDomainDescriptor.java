/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.impl.internal.domain;

import static java.util.Collections.emptySet;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;

import java.io.File;

/**
 * Represents the description of a domain when none is given
 */
public class EmptyDomainDescriptor extends DomainDescriptor {

  public EmptyDomainDescriptor(File domainLocation) {
    super(domainLocation.getName());
    this.setArtifactLocation(domainLocation);
    setArtifactLocation(domainLocation);
    setRootFolder(domainLocation.getParentFile());
    setConfigResources(emptySet());
  }
}
