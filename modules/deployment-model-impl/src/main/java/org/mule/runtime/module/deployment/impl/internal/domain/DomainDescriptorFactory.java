/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.domain;

import static org.mule.runtime.core.api.config.MuleManifest.getProductVersion;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.DOMAIN;
import static org.mule.runtime.deployment.model.api.domain.DomainDescriptor.DEFAULT_CONFIGURATION_RESOURCE;
import static org.mule.runtime.deployment.model.api.domain.DomainDescriptor.MULE_DOMAIN_CLASSIFIER;
import static org.mule.runtime.module.deployment.impl.internal.maven.MavenBundleDescriptorLoader.OVERRIDE_ARTIFACT_ID_KEY;

import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MuleDomainModel;
import org.mule.runtime.api.deployment.persistence.AbstractMuleArtifactModelJsonSerializer;
import org.mule.runtime.api.deployment.persistence.MuleDomainModelJsonSerializer;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DescriptorLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.artifact.AbstractDeployableDescriptorFactory;
import org.mule.runtime.module.deployment.impl.internal.maven.MavenBundleDescriptorLoader;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorLoader;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * Creates artifact descriptor for application
 */
public class DomainDescriptorFactory extends AbstractDeployableDescriptorFactory<MuleDomainModel, DomainDescriptor> {

  public static final String OVERRIDE_DOMAIN_ARTIFACT_ID = "domain.override.artifactId";

  public DomainDescriptorFactory(ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader,
                                 DescriptorLoaderRepository descriptorLoaderRepository,
                                 ArtifactDescriptorValidatorBuilder artifactDescriptorValidatorBuilder) {
    super(artifactPluginDescriptorLoader, descriptorLoaderRepository, artifactDescriptorValidatorBuilder);
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
  protected void doDescriptorConfig(MuleDomainModel artifactModel, DomainDescriptor descriptor, File artifactLocation) {
    super.doDescriptorConfig(artifactModel, descriptor, artifactLocation);
  }

  @Override
  protected DomainDescriptor createArtifactDescriptor(File artifactLocation, String name,
                                                      Optional<Properties> deploymentProperties) {
    return new DomainDescriptor(artifactLocation.getName(), deploymentProperties);
  }

  protected Map<String, Object> getBundleDescriptorAttributes(MuleArtifactLoaderDescriptor bundleDescriptorLoader,
                                                              Optional<Properties> deploymentPropertiesOptional) {
    Map<String, Object> attributes = super.getBundleDescriptorAttributes(bundleDescriptorLoader, deploymentPropertiesOptional);
    if (deploymentPropertiesOptional.isPresent()) {
      Properties properties = deploymentPropertiesOptional.get();
      if (properties.containsKey(OVERRIDE_DOMAIN_ARTIFACT_ID)) {
        attributes.put(OVERRIDE_ARTIFACT_ID_KEY, properties.getProperty(OVERRIDE_DOMAIN_ARTIFACT_ID));
      }
    }
    return attributes;
  }

  public static BundleDescriptor createBundleDescriptorFromName(String domainName) {
    BundleDescriptor.Builder builder = new BundleDescriptor.Builder()
        .setArtifactId(domainName)
        .setVersion(getProductVersion())
        .setGroupId("org.mule.runtime")
        .setClassifier(MULE_DOMAIN_CLASSIFIER);
    return builder.build();
  }
}
