/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.domain;

import static org.mule.runtime.container.api.MuleFoldersUtil.getDomainFolder;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorFactoryProvider.artifactDescriptorFactoryProvider;
import static org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor.DEFAULT_CONFIGURATION_RESOURCE;

import static java.util.Optional.empty;

import org.mule.runtime.deployment.model.api.artifact.DescriptorLoaderRepositoryFactory;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;
import org.mule.runtime.module.deployment.impl.internal.artifact.DeployableArtifactDescriptorFactoryTestCase;
import org.mule.runtime.module.deployment.impl.internal.builder.DomainFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorLoader;

import java.io.File;
import java.net.URISyntaxException;

public class DomainDescriptorFactoryTestCase
    extends DeployableArtifactDescriptorFactoryTestCase<DomainDescriptor, DomainFileBuilder> {

  @Override
  protected DomainDescriptor createArtifactDescriptor(String domainPath) throws URISyntaxException {
    final DomainDescriptorFactory DomainDescriptorFactory = createDeployableDescriptorFactory();
    return DomainDescriptorFactory.create(getArtifact(domainPath), empty());
  }

  @Override
  protected File getArtifact(String domainPath) throws URISyntaxException {
    return new File(getClass().getClassLoader().getResource(domainPath).toURI());
  }

  @Override
  protected DomainFileBuilder createArtifactFileBuilder() {
    return new DomainFileBuilder(ARTIFACT_NAME);
  }

  @Override
  protected String getArtifactRootFolder() {
    return "domains";
  }

  @Override
  protected String getDefaultConfigurationResourceLocation() {
    return DEFAULT_CONFIGURATION_RESOURCE;
  }

  @Override
  protected File getArtifactFolder() {
    return getDomainFolder(ARTIFACT_NAME);
  }

  @Override
  protected DomainDescriptor createArtifactDescriptor() {
    final DomainDescriptorFactory artifactDescriptorFactory = createDeployableDescriptorFactory();
    return artifactDescriptorFactory.create(getArtifactFolder(), empty());
  }

  @Override
  protected DomainDescriptorFactory createDeployableDescriptorFactory() {
    return new DomainDescriptorFactory(new ArtifactPluginDescriptorLoader(artifactDescriptorFactoryProvider()
        .createArtifactPluginDescriptorFactory(new DescriptorLoaderRepositoryFactory().createDescriptorLoaderRepository(),
                                               ArtifactDescriptorValidatorBuilder.builder())),
                                       createDescriptorLoaderRepository(), ArtifactDescriptorValidatorBuilder.builder());
  }
}
