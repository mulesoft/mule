/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.policy;

import static org.mule.runtime.api.artifact.ArtifactType.POLICY;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import static java.util.stream.Collectors.toSet;

import org.mule.runtime.api.artifact.ArtifactType;
import org.mule.runtime.api.deployment.meta.MulePolicyModel;
import org.mule.runtime.api.deployment.persistence.AbstractMuleArtifactModelJsonSerializer;
import org.mule.runtime.api.deployment.persistence.MulePolicyModelJsonSerializer;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.deployment.model.api.artifact.DescriptorLoaderRepositoryFactory;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.AbstractArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidator;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfigurationLoader;
import org.mule.runtime.module.artifact.api.descriptor.DescriptorLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorFactory;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorLoader;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * Creates descriptors for policy templates
 */
public class PolicyTemplateDescriptorFactory
    extends AbstractArtifactDescriptorFactory<MulePolicyModel, PolicyTemplateDescriptor> {

  private final ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader;

  /**
   * Creates a default factory
   */
  @SuppressWarnings({"unused"})
  public PolicyTemplateDescriptorFactory() {
    this(new ArtifactPluginDescriptorLoader(new ArtifactPluginDescriptorFactory()),
         new DescriptorLoaderRepositoryFactory().createDescriptorLoaderRepository(),
         ArtifactDescriptorValidatorBuilder.builder());
  }

  /**
   * Creates a new factory
   *
   * @param artifactPluginDescriptorLoader     loads the artifact descriptor for plugins used on the policy template. Non null
   * @param descriptorLoaderRepository         contains all the {@link ClassLoaderConfigurationLoader} registered on the
   *                                           container. Non null
   * @param artifactDescriptorValidatorBuilder {@link ArtifactDescriptorValidatorBuilder} to create the
   *                                           {@link ArtifactDescriptorValidator} in order to check the state of the descriptor
   *                                           once loaded.
   */
  public PolicyTemplateDescriptorFactory(ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader,
                                         DescriptorLoaderRepository descriptorLoaderRepository,
                                         ArtifactDescriptorValidatorBuilder artifactDescriptorValidatorBuilder) {
    super(descriptorLoaderRepository, artifactDescriptorValidatorBuilder);

    checkArgument(artifactPluginDescriptorLoader != null, "artifactPluginDescriptorLoader cannot be null");
    this.artifactPluginDescriptorLoader = artifactPluginDescriptorLoader;
  }

  @Override
  protected void doDescriptorConfig(MulePolicyModel artifactModel, PolicyTemplateDescriptor descriptor, File artifactLocation) {
    descriptor.setRootFolder(artifactLocation);
    descriptor.setPlugins(parseArtifactPluginDescriptors(descriptor));
    descriptor.setSupportedJavaVersions(artifactModel.getSupportedJavaVersions());
  }

  private Set<ArtifactPluginDescriptor> parseArtifactPluginDescriptors(PolicyTemplateDescriptor descriptor) {
    Set<BundleDependency> pluginDependencies = descriptor.getClassLoaderConfiguration().getDependencies().stream()
        .filter(dependency -> dependency.getDescriptor().isPlugin()).collect(toSet());

    return pluginDependencies.stream().map(dependency -> {
      try {
        return artifactPluginDescriptorLoader.load(new File(dependency.getBundleUri()), dependency.getDescriptor(), descriptor);
      } catch (IOException e) {
        throw new MuleRuntimeException(e);
      }
    }).collect(toSet());
  }

  @Override
  protected AbstractMuleArtifactModelJsonSerializer<MulePolicyModel> getMuleArtifactModelJsonSerializer() {
    return new MulePolicyModelJsonSerializer();
  }

  @Override
  protected ArtifactType getArtifactType() {
    return POLICY;
  }

  @Override
  protected PolicyTemplateDescriptor createArtifactDescriptor(File artifactLocation, String name,
                                                              Optional<Properties> deploymentProperties) {
    return new PolicyTemplateDescriptor(name, deploymentProperties);
  }
}
