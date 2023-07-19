/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.service.internal.artifact;

import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.SERVICE;
import org.mule.runtime.api.deployment.meta.MuleServiceModel;
import org.mule.runtime.api.deployment.persistence.AbstractMuleArtifactModelJsonSerializer;
import org.mule.runtime.api.deployment.persistence.MuleServiceModelJsonSerializer;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.module.artifact.api.descriptor.AbstractArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidator;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfigurationLoader;
import org.mule.runtime.module.artifact.api.descriptor.DescriptorLoaderRepository;
import org.mule.runtime.module.service.api.artifact.ServiceDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder;

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
   * @param descriptorLoaderRepository         contains all the {@link ClassLoaderConfigurationLoader} registered on the
   *                                           container. Non null
   * @param artifactDescriptorValidatorBuilder {@link ArtifactDescriptorValidatorBuilder} to create the
   *                                           {@link ArtifactDescriptorValidator} in order to check the state of the descriptor
   *                                           once loaded.
   */
  public ServiceDescriptorFactory(DescriptorLoaderRepository descriptorLoaderRepository,
                                  ArtifactDescriptorValidatorBuilder artifactDescriptorValidatorBuilder) {
    super(descriptorLoaderRepository,
          artifactDescriptorValidatorBuilder.doNotFailIfBundleDescriptorNotPresentWhenValidationVersionFormat());
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
  protected void doDescriptorConfig(MuleServiceModel artifactModel, ServiceDescriptor descriptor, File artifactLocation) {
    descriptor.setContractModels(artifactModel.getContracts());
  }

  @Override
  protected ServiceDescriptor createArtifactDescriptor(File artifactLocation, String name, Optional<Properties> properties) {
    return new ServiceDescriptor(name);
  }

  @Override
  protected AbstractMuleArtifactModelJsonSerializer<MuleServiceModel> getMuleArtifactModelJsonSerializer() {
    return new MuleServiceModelJsonSerializer();
  }
}
