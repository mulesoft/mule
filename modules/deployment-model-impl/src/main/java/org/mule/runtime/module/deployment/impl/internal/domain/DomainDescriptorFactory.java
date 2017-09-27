/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.domain;

import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.DOMAIN;
import static org.mule.runtime.deployment.model.api.domain.DomainDescriptor.DEFAULT_CONFIGURATION_RESOURCE;

import java.io.File;
import java.util.Optional;
import java.util.Properties;

import org.mule.runtime.api.deployment.meta.MuleDomainModel;
import org.mule.runtime.api.deployment.persistence.AbstractMuleArtifactModelJsonSerializer;
import org.mule.runtime.api.deployment.persistence.MuleDomainModelJsonSerializer;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DescriptorLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.artifact.AbstractDeployableDescriptorFactory;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorLoader;

/**
 * Creates artifact descriptor for application
 */
public class DomainDescriptorFactory extends AbstractDeployableDescriptorFactory<MuleDomainModel, DomainDescriptor> {

  public DomainDescriptorFactory(ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader,
                                 DescriptorLoaderRepository descriptorLoaderRepository) {
    super(artifactPluginDescriptorLoader, descriptorLoaderRepository);
  }


  @Override
  protected String getDefaultConfigurationResource() {
    return DEFAULT_CONFIGURATION_RESOURCE;
  }

  @Override
  protected ArtifactType getArtifactType() {
    return DOMAIN;
  }

  @Override
  protected AbstractMuleArtifactModelJsonSerializer<MuleDomainModel> getMuleArtifactModelJsonSerializer() {
    return new MuleDomainModelJsonSerializer();
  }

  @Override
  protected DomainDescriptor createArtifactDescriptor(File artifactLocation, String name,
                                                      Optional<Properties> deploymentProperties) {
    return new DomainDescriptor(artifactLocation.getName(), deploymentProperties);
  }
}
