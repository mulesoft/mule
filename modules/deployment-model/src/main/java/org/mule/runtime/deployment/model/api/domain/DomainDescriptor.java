/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.domain;

import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor;

import com.google.common.collect.ImmutableSet;

/**
 * Represents the description of a domain.
 */
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
   * @param name domain name. Non empty.
   * @param deploymentProperties deploymentProperties
   */
  public DomainDescriptor(String name, Optional<Properties> deploymentProperties) {
    super(name, deploymentProperties);
  }

  @Override
  protected Set<String> getDefaultConfigResources() {
    return ImmutableSet.<String>builder().add(DEFAULT_CONFIGURATION_RESOURCE).build();
  }
}
