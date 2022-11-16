/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.policy;

import static java.util.Collections.emptySet;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.core.api.util.boot.ExtensionLoaderUtils.isParallelExtensionModelLoadingEnabled;
import static org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelDiscoverer.discoverRuntimeExtensionModels;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionDiscoveryRequest;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelDiscoverer;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelLoaderRepository;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginClassLoaderSupplier;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.extension.api.manager.ExtensionManagerFactory;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Creates {@link ExtensionManager} for mule artifacts that own a {@link MuleContext}
 */
public class ArtifactExtensionManagerFactory implements ExtensionManagerFactory {

  private static final BiFunction<PluginClassLoaderSupplier, ExtensionModelLoaderRepository, ExtensionModelDiscoverer> EXT_MODEL_DISCOVERER =
      ExtensionModelDiscoverer::defaultExtensionModelDiscoverer;

  private final Set<ArtifactPluginDescriptor> artifactPluginsDescriptors;
  private final ExtensionManagerFactory extensionManagerFactory;
  private final ExtensionModelDiscoverer extensionModelDiscoverer;

  /**
   * Creates a extensionManager factory
   *
   * @param artifactPlugins                artifact plugins deployed inside the artifact. Non null.
   * @param extensionModelLoaderRepository {@link ExtensionModelLoaderRepository} with the available extension loaders. Non null.
   * @param extensionManagerFactory        creates the {@link ExtensionManager} for the artifact. Non null
   */
  public ArtifactExtensionManagerFactory(List<ArtifactPlugin> artifactPlugins,
                                         ExtensionModelLoaderRepository extensionModelLoaderRepository,
                                         ExtensionManagerFactory extensionManagerFactory) {
    this(artifactPlugins, extensionModelLoaderRepository, extensionManagerFactory, empty());
  }

  /**
   * Creates a extensionManager factory
   *
   * @param artifactPlugins                artifact plugins deployed inside the artifact. Non null.
   * @param extensionModelLoaderRepository {@link ExtensionModelLoaderRepository} with the available extension loaders. Non null.
   * @param extensionManagerFactory        creates the {@link ExtensionManager} for the artifact. Non null
   * @param extModelDiscovererOverride     overrides how the the extension models for plugins in a class loader are calculated.
   */
  public ArtifactExtensionManagerFactory(List<ArtifactPlugin> artifactPlugins,
                                         ExtensionModelLoaderRepository extensionModelLoaderRepository,
                                         ExtensionManagerFactory extensionManagerFactory,
                                         Optional<BiFunction<PluginClassLoaderSupplier, ExtensionModelLoaderRepository, ExtensionModelDiscoverer>> extModelDiscovererOverride) {
    this.extensionManagerFactory = extensionManagerFactory;
    Map<ArtifactPluginDescriptor, ArtifactClassLoader> artifactPluginsClassLoaders = artifactPlugins
        .stream()
        .collect(toMap(ArtifactPlugin::getDescriptor, ArtifactPlugin::getArtifactClassLoader,
                       (x, y) -> y, LinkedHashMap::new));
    this.artifactPluginsDescriptors = artifactPluginsClassLoaders.keySet();
    this.extensionModelDiscoverer =
        extModelDiscovererOverride.orElse(EXT_MODEL_DISCOVERER).apply(artifactPluginsClassLoaders::get,
                                                                      extensionModelLoaderRepository);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ExtensionManager create(MuleContext muleContext) {
    return create(muleContext, emptySet());
  }

  protected ExtensionManager create(MuleContext muleContext, Set<ExtensionModel> parentArtifactExtensions) {
    final ExtensionManager extensionManager = extensionManagerFactory.create(muleContext);
    final Set<ExtensionModel> extensions = new HashSet<>();
    discoverRuntimeExtensionModels()
        .forEach(extensionManager::registerExtension);
    extensions.addAll(extensionModelDiscoverer
        .discoverPluginsExtensionModels(
                                        ExtensionDiscoveryRequest.builder()
                                            .setArtifactPlugins(artifactPluginsDescriptors)
                                            .setParallelDiscovery(isParallelExtensionModelLoadingEnabled())
                                            .setParentArtifactExtensions(parentArtifactExtensions)
                                            .build()));
    extensions.forEach(extensionManager::registerExtension);
    return extensionManager;
  }

}
