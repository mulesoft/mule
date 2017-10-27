/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.service.internal.artifact;

import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.SERVICE;
import org.mule.runtime.api.deployment.meta.MuleServiceModel;
import org.mule.runtime.api.deployment.persistence.AbstractMuleArtifactModelJsonSerializer;
import org.mule.runtime.api.deployment.persistence.MuleServiceModelJsonSerializer;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.module.artifact.api.descriptor.AbstractArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModelLoader;
import org.mule.runtime.module.artifact.api.descriptor.DescriptorLoaderRepository;

import java.io.File;
import java.util.Optional;
import java.util.Properties;

/**
 * Creates {@link ServiceDescriptor} instances.
 */
public class ServiceDescriptorFactory extends AbstractArtifactDescriptorFactory<MuleServiceModel, ServiceDescriptor> {

  /**
   * Creates a new factory
   *
   * @param descriptorLoaderRepository contains all the {@link ClassLoaderModelLoader} registered on the container. Non null
   */
  public ServiceDescriptorFactory(DescriptorLoaderRepository descriptorLoaderRepository) {
    super(descriptorLoaderRepository);
  }

  @Override
  protected ArtifactType getArtifactType() {
    return SERVICE;
  }

  @Override
  public ServiceDescriptor create(File artifactFolder, Optional<Properties> properties) throws ArtifactDescriptorCreateException {
    if (!artifactFolder.exists()) {
      throw new IllegalArgumentException("Service folder does not exists: " + artifactFolder.getAbsolutePath());
    }

    return super.create(artifactFolder, properties);
  }

  @Override
  protected void validateVersion(ServiceDescriptor descriptor) {
    if (descriptor.getBundleDescriptor() != null) {
      super.validateVersion(descriptor);
    }
  }

  @Override
  protected void doDescriptorConfig(MuleServiceModel artifactModel, ServiceDescriptor descriptor, File artifactLocation) {
    descriptor.setServiceProviderClassName(artifactModel.getServiceProviderClassName());
  }

  @Override
  protected ServiceDescriptor createArtifactDescriptor(File artifactLocation, String name, Optional<Properties> properties) {
    return new ServiceDescriptor(name, properties);
  }

  @Override
  protected AbstractMuleArtifactModelJsonSerializer<MuleServiceModel> getMuleArtifactModelJsonSerializer() {
    return new MuleServiceModelJsonSerializer();
  }
}
