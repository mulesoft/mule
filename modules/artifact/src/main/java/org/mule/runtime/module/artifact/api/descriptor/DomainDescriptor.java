/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.descriptor;

import org.mule.api.annotation.NoExtend;

import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

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
    return ImmutableSet.<String>builder().add(DEFAULT_CONFIGURATION_RESOURCE).build();
  }
}
