/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.artifact.extension;

import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toSet;

import static com.google.common.collect.ImmutableSet.of;

import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.extension.MuleExtensionModelProvider;
import org.mule.runtime.core.api.extension.RuntimeExtensionModelProvider;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.api.plugin.LoaderDescriber;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

/**
 * Discover the {@link ExtensionModel} based on the {@link ExtensionModelLoader} type.
 *
 * @since 4.0
 */
public class ExtensionModelDiscoverer {

  private static final Set<String> LOADER_IDS_NOT_REQUIRING_DEPENDENCIES_DSL = of("crafted", "java", "soap");

  /**
   * For each artifactPlugin discovers the {@link ExtensionModel}.
   *
   * @param loaderRepository {@link ExtensionModelLoaderRepository} with the available extension loaders.
   * @param artifactPlugins  {@link Pair} of {@link ArtifactPluginDescriptor} and {@link ArtifactClassLoader} for artifact plugins
   *                         deployed inside the artifact. Non null.
   * @return {@link Set} of {@link Pair} carrying the {@link ArtifactPluginDescriptor} and it's corresponding
   *         {@link ExtensionModel}.
   * 
   * @deprecated from 4.5 use {@link #discoverPluginsExtensionModels(ExtensionDiscoveryRequest)} instead.
   */
  @Deprecated
  public Set<Pair<ArtifactPluginDescriptor, ExtensionModel>> discoverPluginsExtensionModels(ExtensionModelLoaderRepository loaderRepository,
                                                                                            List<Pair<ArtifactPluginDescriptor, ArtifactClassLoader>> artifactPlugins) {
    return discoverPluginsExtensionModels(ExtensionDiscoveryRequest.builder()
        .setLoaderRepository(loaderRepository)
        .setArtifactPlugins(artifactPlugins)
        .build());
  }

  /**
   * For each artifactPlugin discovers the {@link ExtensionModel}.
   *
   * @param loaderRepository         {@link ExtensionModelLoaderRepository} with the available extension loaders.
   * @param artifactPlugins          {@link Pair} of {@link ArtifactPluginDescriptor} and {@link ArtifactClassLoader} for artifact
   *                                 plugins deployed inside the artifact. Non null.
   * @param parentArtifactExtensions {@link Set} of {@link ExtensionModel} to also take into account when parsing extensions
   * @return {@link Set} of {@link Pair} carrying the {@link ArtifactPluginDescriptor} and it's corresponding
   *         {@link ExtensionModel}.
   * 
   * @deprecated form 4.5 use {@link #discoverPluginsExtensionModels(ExtensionDiscoveryRequest)} instead.
   */
  @Deprecated
  public Set<Pair<ArtifactPluginDescriptor, ExtensionModel>> discoverPluginsExtensionModels(ExtensionModelLoaderRepository loaderRepository,
                                                                                            List<Pair<ArtifactPluginDescriptor, ArtifactClassLoader>> artifactPlugins,
                                                                                            Set<ExtensionModel> parentArtifactExtensions) {
    return discoverPluginsExtensionModels(ExtensionDiscoveryRequest.builder()
        .setLoaderRepository(loaderRepository)
        .setArtifactPlugins(artifactPlugins)
        .setParentArtifactExtensions(parentArtifactExtensions)
        .build());
  }

  /**
   * For each artifactPlugin discovers the {@link ExtensionModel}.
   *
   * @param discoveryRequest an object containing the parameterization of the discovery process.
   * @return {@link Set} of {@link Pair} carrying the {@link ArtifactPluginDescriptor} and it's corresponding
   *         {@link ExtensionModel}.
   */
  public Set<Pair<ArtifactPluginDescriptor, ExtensionModel>> discoverPluginsExtensionModels(ExtensionDiscoveryRequest discoveryRequest) {
    final Set<Pair<ArtifactPluginDescriptor, ExtensionModel>> descriptorsWithExtensions = new HashSet<>();

    if (discoveryRequest.isParallelDiscovery()) {
      discoveryRequest.getArtifactPlugins()
          .parallelStream()
          .filter(artifactPlugin -> !requiresDependencyDsl(artifactPlugin))
          .forEach(artifactPlugin -> discoverPluginExtensionModel(discoveryRequest,
                                                                  discoveryRequest.getParentArtifactExtensions(),
                                                                  descriptorsWithExtensions, artifactPlugin));

      discoveryRequest.getArtifactPlugins()
          // an xml-sdk extension may use the dsl of another xml-sdk extension, so these cannot be parallelized
          .stream()
          .filter(artifactPlugin -> requiresDependencyDsl(artifactPlugin))
          .forEach(artifactPlugin -> {
            Set<ExtensionModel> extensions = descriptorsWithExtensions.stream().map(Pair::getSecond).collect(toSet());
            extensions.addAll(discoveryRequest.getParentArtifactExtensions());
            discoverPluginExtensionModel(discoveryRequest, extensions, descriptorsWithExtensions, artifactPlugin);
          });
    } else {
      discoveryRequest.getArtifactPlugins()
          .stream()
          .forEach(artifactPlugin -> {
            Set<ExtensionModel> extensions = descriptorsWithExtensions.stream().map(Pair::getSecond).collect(toSet());
            extensions.addAll(discoveryRequest.getParentArtifactExtensions());
            discoverPluginExtensionModel(discoveryRequest, extensions, descriptorsWithExtensions, artifactPlugin);
          });
    }

    return descriptorsWithExtensions;
  }

  protected boolean requiresDependencyDsl(Pair<ArtifactPluginDescriptor, ArtifactClassLoader> artifactPlugin) {
    return artifactPlugin.getFirst().getExtensionModelDescriptorProperty()
        .map(extModelDescriptorProperty -> extModelDescriptorProperty.getId())
        .map(loaderId -> !LOADER_IDS_NOT_REQUIRING_DEPENDENCIES_DSL.contains(loaderId))
        .orElse(true);
  }

  protected void discoverPluginExtensionModel(ExtensionDiscoveryRequest discoveryRequest, Set<ExtensionModel> extensions,
                                              final Set<Pair<ArtifactPluginDescriptor, ExtensionModel>> descriptorsWithExtensions,
                                              Pair<ArtifactPluginDescriptor, ArtifactClassLoader> artifactPlugin) {
    final ArtifactPluginDescriptor artifactPluginDescriptor = artifactPlugin.getFirst();
    Optional<LoaderDescriber> loaderDescriber = artifactPluginDescriptor.getExtensionModelDescriptorProperty();
    ClassLoader artifactClassloader = artifactPlugin.getSecond().getClassLoader();
    String artifactName = artifactPluginDescriptor.getName();
    ExtensionModel extension = loaderDescriber
        .map(describer -> discoverExtensionThroughJsonDescriber(discoveryRequest.getLoaderRepository(), describer,
                                                                extensions, artifactClassloader,
                                                                artifactName,
                                                                discoveryRequest.isEnrichDescriptions()
                                                                    ? emptyMap()
                                                                    : singletonMap("EXTENSION_LOADER_DISABLE_DESCRIPTIONS_ENRICHMENT",
                                                                                   true)))
        .orElse(null);
    if (extension != null) {
      descriptorsWithExtensions.add(new Pair<>(artifactPluginDescriptor, extension));
    }
  }

  /**
   * Discover the extension models provided by the runtime.
   *
   * @return {@link Set} of the runtime provided {@link ExtensionModel}s.
   */
  public Set<ExtensionModel> discoverRuntimeExtensionModels() {
    return new SpiServiceRegistry()
        .lookupProviders(RuntimeExtensionModelProvider.class, currentThread().getContextClassLoader())
        .stream()
        .map(RuntimeExtensionModelProvider::createExtensionModel)
        .collect(toSet());
  }

  /**
   * Looks for an extension using the mule-artifact.json file, where if available it will parse it using the
   * {@link ExtensionModelLoader} which {@link ExtensionModelLoader#getId() ID} matches the plugin's descriptor ID.
   *
   * @param extensionModelLoaderRepository {@link ExtensionModelLoaderRepository} with the available extension loaders.
   * @param loaderDescriber                a descriptor that contains parameterization to construct an {@link ExtensionModel}
   * @param extensions                     with the previously generated {@link ExtensionModel}s that will be used to generate the
   *                                       current {@link ExtensionModel} and store it in {@code extensions} once generated.
   * @param artifactClassloader            the loaded artifact {@link ClassLoader} to find the required resources.
   * @param artifactName                   the name of the artifact being loaded.
   * @throws IllegalArgumentException there is no {@link ExtensionModelLoader} for the ID in the {@link MulePluginModel}.
   */
  private ExtensionModel discoverExtensionThroughJsonDescriber(ExtensionModelLoaderRepository extensionModelLoaderRepository,
                                                               LoaderDescriber loaderDescriber, Set<ExtensionModel> extensions,
                                                               ClassLoader artifactClassloader, String artifactName,
                                                               Map<String, Object> additionalAttributes) {
    ExtensionModelLoader loader = extensionModelLoaderRepository.getExtensionModelLoader(loaderDescriber)
        .orElseThrow(() -> new IllegalArgumentException(format("The identifier '%s' does not match with the describers available "
            + "to generate an ExtensionModel (working with the plugin '%s')", loaderDescriber.getId(), artifactName)));
    if (!extensions.contains(MuleExtensionModelProvider.getExtensionModel())) {
      extensions = ImmutableSet.<ExtensionModel>builder().addAll(extensions).addAll(discoverRuntimeExtensionModels()).build();
    }
    Map<String, Object> attributes = new HashMap<>(loaderDescriber.getAttributes());
    attributes.putAll(additionalAttributes);
    return loader.loadExtensionModel(artifactClassloader, getDefault(extensions), attributes);
  }
}
