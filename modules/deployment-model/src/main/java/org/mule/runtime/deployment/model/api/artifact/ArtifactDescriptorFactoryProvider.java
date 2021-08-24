/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.artifact;

import static java.util.ServiceLoader.load;

import org.mule.runtime.api.deployment.meta.MuleApplicationModel;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.api.plugin.resolver.PluginDependenciesResolver;
import org.mule.runtime.module.artifact.api.descriptor.AbstractArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModelLoader;
import org.mule.runtime.module.artifact.api.descriptor.DescriptorLoaderRepository;

/**
 * Provides concrete implementations of {@link PluginDependenciesResolver} and {@link AbstractArtifactDescriptorFactory}.
 * 
 * @since 4.5
 */
public interface ArtifactDescriptorFactoryProvider {

  final LazyValue<ArtifactDescriptorFactoryProvider> INSTANCE =
      new LazyValue<>(() -> load(ArtifactDescriptorFactoryProvider.class,
                                 ArtifactDescriptorFactoryProvider.class.getClassLoader())
                                     .iterator().next());

  public static ArtifactDescriptorFactoryProvider artifactDescriptorFactoryProvider() {
    return INSTANCE.get();
  }

  /**
   * Assembly the complete list of artifacts, while sorting them in a lexicographic order by name to then resolve sanitize the
   * exported packages and resource by the plugin's dependencies (avoids exporting elements that are already exported by other
   * plugin).
   *
   * @param artifactPluginDescriptorFactory factory to create {@link ArtifactPluginDescriptor} when there's a missing dependency
   *                                        to resolve
   */
  PluginDependenciesResolver createBundlePluginDependenciesResolver(ArtifactDescriptorFactory<ArtifactPluginDescriptor> artifactPluginDescriptorFactory);

  /**
   * Creates a descriptor factory for describing mule applications.
   * 
   * @param artifactPluginDescriptorFactory    {@link ArtifactDescriptorFactory} to create the descriptor for plugins.
   * @param descriptorLoaderRepository         {@link DescriptorLoaderRepository} to get the descriptor loader implementation.
   * @param artifactDescriptorValidatorBuilder
   * @return a factory to create {@link ApplicationDescriptor}s.
   */
  AbstractArtifactDescriptorFactory<MuleApplicationModel, ApplicationDescriptor> createApplicationDescriptorFactory(ArtifactDescriptorFactory<ArtifactPluginDescriptor> artifactPluginDescriptorFactory,
                                                                                                                    DescriptorLoaderRepository descriptorLoaderRepository,
                                                                                                                    ArtifactDescriptorValidatorBuilder artifactDescriptorValidatorBuilder);

  /**
   * Creates a descriptor factory for describing mule artifact plugins.
   * 
   * @param descriptorLoaderRepository         contains all the {@link ClassLoaderModelLoader} registered on the container. Non
   *                                           null
   * @param artifactDescriptorValidatorBuilder {@link ArtifactDescriptorValidatorBuilder} builder to define the validator to be
   *                                           used. Non null.
   * @return a factory to create {@link ArtifactPluginDescriptor}s.
   */
  AbstractArtifactDescriptorFactory<MulePluginModel, ArtifactPluginDescriptor> createArtifactPluginDescriptorFactory(DescriptorLoaderRepository descriptorLoaderRepository,
                                                                                                                     ArtifactDescriptorValidatorBuilder artifactDescriptorValidatorBuilder);
}
