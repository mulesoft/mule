/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.descriptor;

import static java.util.Collections.singleton;

import org.mule.api.annotation.NoExtend;

import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * Represents the description of a domain.
 *
 * @since 4.5
 */
@NoExtend
public class DomainDescriptor extends DeployableArtifactDescriptor {

  public static final String DEFAULT_DOMAIN_NAME = "default";
  public static final String DEFAULT_CONFIGURATION_RESOURCE = "mule-domain-config.xml";
  public static final String MULE_DOMAIN_CLASSIFIER = "mule-domain";

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

  @Override
  protected Set<String> getDefaultConfigResources() {
    return singleton(DEFAULT_CONFIGURATION_RESOURCE);
  }
}
