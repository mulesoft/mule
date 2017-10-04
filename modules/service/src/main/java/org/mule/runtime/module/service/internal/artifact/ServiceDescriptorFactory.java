/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.service.internal.artifact;

import static java.util.Collections.emptyMap;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.SERVICE;
import static org.mule.runtime.module.service.internal.artifact.ServiceDescriptor.SERVICE_PROPERTIES;
import org.mule.runtime.api.deployment.meta.MuleServiceModel;
import org.mule.runtime.api.deployment.persistence.AbstractMuleArtifactModelJsonSerializer;
import org.mule.runtime.api.deployment.persistence.MuleServiceModelJsonSerializer;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.module.artifact.api.descriptor.AbstractArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModelLoader;
import org.mule.runtime.module.artifact.api.descriptor.DescriptorLoaderRepository;
import org.mule.runtime.module.artifact.api.descriptor.InvalidDescriptorLoaderException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

/**
 * Creates {@link ServiceDescriptor} instances.
 */
public class ServiceDescriptorFactory extends AbstractArtifactDescriptorFactory<MuleServiceModel, ServiceDescriptor> {

  private static final String SERVICE_PROVIDER_CLASS_NAME = "service.className";

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
    // TODO(pablo.kraan): MULE-13281 - remove properties descriptor support once all the services are migrated to the new file
    // format
    final File servicePropsFile = new File(artifactFolder, SERVICE_PROPERTIES);
    if (servicePropsFile.exists()) {
      final String serviceName = artifactFolder.getName();
      final ServiceDescriptor descriptor = new ServiceDescriptor(serviceName);
      descriptor.setRootFolder(artifactFolder);

      Properties props = new Properties();
      try {
        props.load(new FileReader(servicePropsFile));
      } catch (IOException e) {
        throw new ArtifactDescriptorCreateException("Cannot read service.properties file", e);
      }

      descriptor.setClassLoaderModel(createClassLoaderModel(artifactFolder));

      descriptor.setServiceProviderClassName(props.getProperty(SERVICE_PROVIDER_CLASS_NAME));

      return descriptor;
    }

    return super.create(artifactFolder, properties);
  }

  @Override
  protected void validateVersion(ServiceDescriptor descriptor) {
    if (descriptor.getBundleDescriptor() != null) {
      super.validateVersion(descriptor);
    }
  }

  private ClassLoaderModel createClassLoaderModel(File artifactFolder) {
    try {
      return new LibFolderClassLoaderModelLoader().load(artifactFolder, emptyMap(), ArtifactType.SERVICE);
    } catch (InvalidDescriptorLoaderException e) {
      throw new IllegalStateException("Cannot load classloader model for service", e);
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
