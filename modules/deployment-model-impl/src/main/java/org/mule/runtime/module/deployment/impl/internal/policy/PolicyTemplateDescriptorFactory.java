/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.policy;

import static java.util.stream.Collectors.toSet;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.POLICY;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor.MULE_ARTIFACT_JSON_DESCRIPTOR;
import org.mule.runtime.api.deployment.meta.MulePolicyModel;
import org.mule.runtime.api.deployment.persistence.AbstractMuleArtifactModelJsonSerializer;
import org.mule.runtime.api.deployment.persistence.MulePolicyModelJsonSerializer;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModelLoader;
import org.mule.runtime.module.artifact.api.descriptor.DescriptorLoaderRepository;
import org.mule.runtime.module.artifact.api.descriptor.AbstractArtifactDescriptorFactory;
import org.mule.runtime.module.deployment.impl.internal.artifact.ServiceRegistryDescriptorLoaderRepository;
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

  protected static final String MISSING_POLICY_DESCRIPTOR_ERROR =
      "Policy must contain a " + MULE_ARTIFACT_JSON_DESCRIPTOR + " file";

  private final ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader;

  /**
   * Creates a default factory
   */
  @SuppressWarnings({"unused"})
  public PolicyTemplateDescriptorFactory() {
    this(new ArtifactPluginDescriptorLoader(new ArtifactPluginDescriptorFactory()),
         new ServiceRegistryDescriptorLoaderRepository(new SpiServiceRegistry()));
  }

  /**
   * Creates a new factory
   *
   * @param artifactPluginDescriptorLoader loads the artifact descriptor for plugins used on the policy template. Non null
   * @param descriptorLoaderRepository contains all the {@link ClassLoaderModelLoader} registered on the container. Non null
   */
  public PolicyTemplateDescriptorFactory(ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader,
                                         DescriptorLoaderRepository descriptorLoaderRepository) {
    super(descriptorLoaderRepository);

    checkArgument(artifactPluginDescriptorLoader != null, "artifactPluginDescriptorLoader cannot be null");
    this.artifactPluginDescriptorLoader = artifactPluginDescriptorLoader;
  }

  @Override
  protected void doDescriptorConfig(MulePolicyModel artifactModel, PolicyTemplateDescriptor descriptor, File artifactLocation) {
    descriptor.setRootFolder(artifactLocation);
    descriptor.setPlugins(parseArtifactPluginDescriptors(descriptor));
  }

  private Set<ArtifactPluginDescriptor> parseArtifactPluginDescriptors(PolicyTemplateDescriptor descriptor) {
    Set<BundleDependency> pluginDependencies = descriptor.getClassLoaderModel().getDependencies().stream()
        .filter(dependency -> dependency.getDescriptor().isPlugin()).collect(toSet());

    return pluginDependencies.stream().map(dependency -> {
      try {
        return artifactPluginDescriptorLoader.load(new File(dependency.getBundleUri()));
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
