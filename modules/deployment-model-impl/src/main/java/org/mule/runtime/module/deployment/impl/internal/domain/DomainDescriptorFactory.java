/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.domain;

import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.DOMAIN;
import static org.mule.runtime.deployment.model.api.domain.DomainDescriptor.DEFAULT_CONFIGURATION_RESOURCE;
import static org.mule.runtime.deployment.model.api.domain.DomainDescriptor.DEFAULT_CONFIGURATION_RESOURCE_LOCATION;
import org.mule.runtime.api.deployment.meta.MuleDomainModel;
import org.mule.runtime.api.deployment.persistence.MuleDomainModelJsonSerializer;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;
import org.mule.runtime.module.deployment.impl.internal.artifact.AbstractDeployableDescriptorFactory;
import org.mule.runtime.module.deployment.impl.internal.artifact.DescriptorLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorLoader;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

/**
 * Creates artifact descriptor for application
 */
public class DomainDescriptorFactory extends AbstractDeployableDescriptorFactory<MuleDomainModel, DomainDescriptor> {

  private static final String MULE_DOMAIN_JSON = "mule-domain.json";

  public DomainDescriptorFactory(ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader,
                                 DescriptorLoaderRepository descriptorLoaderRepository) {
    super(artifactPluginDescriptorLoader, descriptorLoaderRepository);
  }

  @Override
  protected String getDescriptorFileName() {
    return MULE_DOMAIN_JSON;
  }

  @Override
  protected void doDescriptorConfig(MuleDomainModel artifactModel, DomainDescriptor descriptor) {
    // Nothing to do
  }

  @Override
  protected DomainDescriptor createArtifactDescriptor(String name) {
    return new DomainDescriptor(name);
  }

  @Override
  protected String getDefaultConfigurationResourceLocation() {
    return DEFAULT_CONFIGURATION_RESOURCE_LOCATION;
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
  protected MuleDomainModel deserializeArtifactModel(InputStream stream) throws IOException {
    return new MuleDomainModelJsonSerializer().deserialize(IOUtils.toString(stream));
  }
}
