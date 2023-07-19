/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.deployment.model.api.domain;

import java.util.Optional;
import java.util.Properties;

/**
 * Represents the description of a domain.
 * 
 * @deprecated since 4.5. use org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor instead.
 */
@Deprecated
public class DomainDescriptor extends org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor {

  /**
   * Creates a new domain descriptor
   *
   * @param name domain name. Non empty.
   */
  public DomainDescriptor(String name) {
    super(name);
  }

  /**
   * Creates a new domain descriptor
   *
   * @param name                 domain name. Non empty.
   * @param deploymentProperties deploymentProperties
   */
  public DomainDescriptor(String name, Optional<Properties> deploymentProperties) {
    super(name, deploymentProperties);
  }

}
