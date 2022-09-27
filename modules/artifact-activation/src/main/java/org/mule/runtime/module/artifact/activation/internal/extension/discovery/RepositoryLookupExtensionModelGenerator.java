/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.extension.discovery;

import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest.builder;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionDiscoveryRequest;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelLoaderRepository;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginClassLoaderSupplier;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.plugin.LoaderDescriber;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Generates an extension model by delegating to the appropriate {@link ExtensionModelLoader}.
 *
 * @since 4.5
 */
public class RepositoryLookupExtensionModelGenerator implements ExtensionModelGenerator {

  private final PluginClassLoaderSupplier classLoaderFactory;
  private final ExtensionModelLoaderRepository extensionModelLoaderRepository;

  public RepositoryLookupExtensionModelGenerator(PluginClassLoaderSupplier classLoaderFactory,
                                                 ExtensionModelLoaderRepository extensionModelLoaderRepository) {
    this.classLoaderFactory = classLoaderFactory;
    this.extensionModelLoaderRepository = extensionModelLoaderRepository;
  }

  @Override
  public ExtensionModel obtainExtensionModel(ExtensionDiscoveryRequest discoveryRequest,
                                             ArtifactPluginDescriptor artifactPluginDescriptor,
                                             Set<ExtensionModel> dependencies) {
    return artifactPluginDescriptor.getExtensionModelDescriptorProperty()
        .map(describer -> discoverExtensionThroughJsonDescriber(extensionModelLoaderRepository,
                                                                describer,
                                                                dependencies,
                                                                () -> classLoaderFactory.get(artifactPluginDescriptor)
                                                                    .getClassLoader(),
                                                                artifactPluginDescriptor.getName(),
                                                                discoveryRequest.isEnrichDescriptions()
                                                                    ? emptyMap()
                                                                    : singletonMap("EXTENSION_LOADER_DISABLE_DESCRIPTIONS_ENRICHMENT",
                                                                                   true),
                                                                artifactPluginDescriptor,
                                                                discoveryRequest.getConfigurationProperties()))
        .orElse(null);
  }

  /**
   * Looks for an extension using the mule-artifact.json file, where if available it will parse it using the
   * {@link ExtensionModelLoader} which {@link ExtensionModelLoader#getId() ID} matches the plugin's descriptor ID.
   *
   * @param extensionModelLoaderRepository {@link ExtensionModelLoaderRepository} with the available extension loaders.
   * @param loaderDescriber                a descriptor that contains parameterization to construct an {@link ExtensionModel}.
   * @param dependencies                   the previously generated {@link ExtensionModel}s that will be used to generate the
   *                                       current {@link ExtensionModel}.
   * @param artifactClassloader            the loaded artifact {@link ClassLoader} to find the required resources.
   * @param artifactName                   the name of the artifact being loaded.
   * @param additionalAttributes           custom parameters for the
   *                                       {@link org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest}.
   * @param artifactPluginDescriptor       {@link ArtifactPluginDescriptor} of the extension.
   * @param configurationProperties        {@link ConfigurationProperties} to take into account in the discovery.
   * @throws IllegalArgumentException there is no {@link ExtensionModelLoader} for the ID in the {@link MulePluginModel}.
   */
  private ExtensionModel discoverExtensionThroughJsonDescriber(ExtensionModelLoaderRepository extensionModelLoaderRepository,
                                                               LoaderDescriber loaderDescriber,
                                                               Set<ExtensionModel> dependencies,
                                                               Supplier<ClassLoader> artifactClassloader,
                                                               String artifactName,
                                                               Map<String, Object> additionalAttributes,
                                                               ArtifactPluginDescriptor artifactPluginDescriptor,
                                                               ConfigurationProperties configurationProperties) {
    ExtensionModelLoader loader = extensionModelLoaderRepository.getExtensionModelLoader(loaderDescriber)
        .orElseThrow(() -> new IllegalArgumentException(format("The identifier '%s' does not match with the describers available "
            + "to generate an ExtensionModel (working with the plugin '%s')", loaderDescriber.getId(), artifactName)));
    Map<String, Object> attributes = new HashMap<>(loaderDescriber.getAttributes());
    attributes.putAll(additionalAttributes);

    return loader.loadExtensionModel(builder(artifactClassloader.get(), getDefault(dependencies), configurationProperties)
        .addParameters(attributes)
        .setArtifactCoordinates(artifactPluginDescriptor.getBundleDescriptor())
        .build());
  }
}
