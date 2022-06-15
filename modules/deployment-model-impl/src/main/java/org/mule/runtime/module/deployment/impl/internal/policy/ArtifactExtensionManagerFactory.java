/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.policy;

import static org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelDiscoverer.defaultExtensionModelDiscoverer;
import static org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelDiscoverer.discoverRuntimeExtensionModels;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toMap;

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
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Creates {@link ExtensionManager} for mule artifacts that own a {@link MuleContext}
 */
public class ArtifactExtensionManagerFactory implements ExtensionManagerFactory {

  private static BiFunction<PluginClassLoaderSupplier, ExtensionModelLoaderRepository, ExtensionModelDiscoverer> EXT_MODEL_DISCOVERER_FACTORY =
      (pcl, eml) -> defaultExtensionModelDiscoverer(pcl, eml);

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
    this.extensionManagerFactory = extensionManagerFactory;
    Map<ArtifactPluginDescriptor, ArtifactClassLoader> artifactPluginsClassLoaders = artifactPlugins
        .stream()
        .collect(toMap(ArtifactPlugin::getDescriptor, ArtifactPlugin::getArtifactClassLoader,
                       (x, y) -> y, LinkedHashMap::new));
    this.artifactPluginsDescriptors = artifactPluginsClassLoaders.keySet();
    this.extensionModelDiscoverer =
        EXT_MODEL_DISCOVERER_FACTORY.apply(artifactPluginsClassLoaders::get, extensionModelLoaderRepository);
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
                                            .setParentArtifactExtensions(parentArtifactExtensions)
                                            .build()));
    extensions.forEach(extensionManager::registerExtension);
    return extensionManager;
  }

}
