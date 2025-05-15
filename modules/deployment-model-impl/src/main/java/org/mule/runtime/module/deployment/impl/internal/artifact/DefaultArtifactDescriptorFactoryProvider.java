/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.artifact;

import org.mule.runtime.api.deployment.meta.MuleApplicationModel;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.deployment.meta.MulePolicyModel;
import org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorFactoryProvider;
import org.mule.runtime.deployment.model.api.plugin.resolver.PluginDependenciesResolver;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.AbstractArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DescriptorLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.application.ApplicationDescriptorFactory;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorFactory;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorLoader;
import org.mule.runtime.module.deployment.impl.internal.plugin.BundlePluginDependenciesResolver;
import org.mule.runtime.module.deployment.impl.internal.policy.PolicyTemplateDescriptorFactory;

/**
 * Implementation of {@link ArtifactDescriptorFactoryProvider} discoverable through SPI.
 *
 * @since 4.5
 */
public class DefaultArtifactDescriptorFactoryProvider implements ArtifactDescriptorFactoryProvider {

  @Override
  public PluginDependenciesResolver createBundlePluginDependenciesResolver(ArtifactDescriptorFactory<ArtifactPluginDescriptor> artifactPluginDescriptorFactory) {
    return new BundlePluginDependenciesResolver(artifactPluginDescriptorFactory);
  }

  @Override
  public AbstractArtifactDescriptorFactory<MuleApplicationModel, ApplicationDescriptor> createApplicationDescriptorFactory(ArtifactDescriptorFactory<ArtifactPluginDescriptor> artifactPluginDescriptorFactory,
                                                                                                                           DescriptorLoaderRepository descriptorLoaderRepository,
                                                                                                                           ArtifactDescriptorValidatorBuilder artifactDescriptorValidatorBuilder) {
    return new ApplicationDescriptorFactory(new ArtifactPluginDescriptorLoader(artifactPluginDescriptorFactory),
                                            descriptorLoaderRepository,
                                            artifactDescriptorValidatorBuilder);
  }

  @Override
  public AbstractArtifactDescriptorFactory<MulePluginModel, ArtifactPluginDescriptor> createArtifactPluginDescriptorFactory(DescriptorLoaderRepository descriptorLoaderRepository,
                                                                                                                            ArtifactDescriptorValidatorBuilder artifactDescriptorValidatorBuilder) {
    return new ArtifactPluginDescriptorFactory(descriptorLoaderRepository, artifactDescriptorValidatorBuilder);
  }

  @Override
  public AbstractArtifactDescriptorFactory<MulePolicyModel, PolicyTemplateDescriptor> createPolicyTemplateDescriptorFactory() {
    return new PolicyTemplateDescriptorFactory();
  }
}
