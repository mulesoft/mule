/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.artifact;

import static java.util.ServiceLoader.load;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.deployment.meta.MuleApplicationModel;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.deployment.meta.MulePolicyModel;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.deployment.model.api.plugin.resolver.PluginDependenciesResolver;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.AbstractArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidator;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfigurationLoader;
import org.mule.runtime.module.artifact.api.descriptor.DescriptorLoaderRepository;

/**
 * Provides concrete implementations of {@link PluginDependenciesResolver} and {@link AbstractArtifactDescriptorFactory}.
 *
 * @since 4.5
 */
@NoImplement
public interface ArtifactDescriptorFactoryProvider {

  LazyValue<ArtifactDescriptorFactoryProvider> INSTANCE =
      new LazyValue<>(() -> load(ArtifactDescriptorFactoryProvider.class,
                                 ArtifactDescriptorFactoryProvider.class.getClassLoader())
                                     .iterator().next());

  static ArtifactDescriptorFactoryProvider artifactDescriptorFactoryProvider() {
    return INSTANCE.get();
  }

  /**
   * Creates a resolver that is used to determine the correct initialization order of the mule-plugin dependencies of an artifact.
   *
   * @param artifactPluginDescriptorFactory factory to create {@link ArtifactPluginDescriptor} when there's a missing dependency
   *                                        to resolve
   */
  PluginDependenciesResolver createBundlePluginDependenciesResolver(ArtifactDescriptorFactory<ArtifactPluginDescriptor> artifactPluginDescriptorFactory);

  /**
   * Creates a factory of descriptors for describing mule applications.
   *
   * @param artifactPluginDescriptorFactory    {@link ArtifactDescriptorFactory} to create the descriptor for plugins.
   * @param descriptorLoaderRepository         {@link DescriptorLoaderRepository} to get the descriptor loader implementation.
   * @param artifactDescriptorValidatorBuilder {@link ArtifactDescriptorValidatorBuilder} to create the
   *                                           {@link ArtifactDescriptorValidator} in order to check the state of the descriptor
   *                                           once loaded.
   * @return a factory to create {@link ApplicationDescriptor}s.
   */
  AbstractArtifactDescriptorFactory<MuleApplicationModel, ApplicationDescriptor> createApplicationDescriptorFactory(ArtifactDescriptorFactory<ArtifactPluginDescriptor> artifactPluginDescriptorFactory,
                                                                                                                    DescriptorLoaderRepository descriptorLoaderRepository,
                                                                                                                    ArtifactDescriptorValidatorBuilder artifactDescriptorValidatorBuilder);

  /**
   * Creates a factory of descriptors for describing mule artifact plugins.
   *
   * @param descriptorLoaderRepository         contains all the {@link ClassLoaderConfigurationLoader} registered on the
   *                                           container. Non null
   * @param artifactDescriptorValidatorBuilder {@link ArtifactDescriptorValidatorBuilder} builder to define the validator to be
   *                                           used. Non null.
   * @return a factory to create {@link ArtifactPluginDescriptor}s.
   */
  AbstractArtifactDescriptorFactory<MulePluginModel, ArtifactPluginDescriptor> createArtifactPluginDescriptorFactory(DescriptorLoaderRepository descriptorLoaderRepository,
                                                                                                                     ArtifactDescriptorValidatorBuilder artifactDescriptorValidatorBuilder);

  /**
   * Creates a factory of descriptors for describing mule policy templates.
   *
   * @return a factory to create {@link PolicyTemplateDescriptor}s.
   *
   * @since 4.6
   */
  AbstractArtifactDescriptorFactory<MulePolicyModel, PolicyTemplateDescriptor> createPolicyTemplateDescriptorFactory();
}
