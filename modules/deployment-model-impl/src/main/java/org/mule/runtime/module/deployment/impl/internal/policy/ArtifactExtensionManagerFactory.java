/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.policy;

import static java.util.stream.Collectors.toSet;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.deployment.impl.internal.artifact.ExtensionModelDiscoverer;
import org.mule.runtime.module.extension.api.manager.ExtensionManagerFactory;
import org.mule.runtime.module.extension.internal.loader.ExtensionModelLoaderRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Creates {@link ExtensionManager} for mule artifacts that own a {@link MuleContext}
 */
public class ArtifactExtensionManagerFactory implements ExtensionManagerFactory {

  private final ExtensionModelLoaderRepository extensionModelLoaderRepository;
  private final List<Pair<ArtifactPluginDescriptor, ArtifactClassLoader>> artifactPlugins;
  private final ExtensionManagerFactory extensionManagerFactory;
  private final ExtensionModelDiscoverer extensionModelDiscoverer;

  /**
   * Creates a extensionManager factory
   *
   * @param artifactPlugins artifact plugins deployed inside the artifact. Non null.
   * @param extensionModelLoaderRepository {@link ExtensionModelLoaderRepository} with the available extension loaders. Non null.
   * @param extensionManagerFactory creates the {@link ExtensionManager} for the artifact. Non null
   */
  public ArtifactExtensionManagerFactory(List<ArtifactPlugin> artifactPlugins,
                                         ExtensionModelLoaderRepository extensionModelLoaderRepository,
                                         ExtensionManagerFactory extensionManagerFactory) {
    this.artifactPlugins = new ArrayList<>();
    artifactPlugins.forEach(artifactPlugin -> this.artifactPlugins
        .add(new Pair(artifactPlugin.getDescriptor(), artifactPlugin.getArtifactClassLoader())));
    this.extensionModelLoaderRepository = extensionModelLoaderRepository;
    this.extensionManagerFactory = extensionManagerFactory;
    this.extensionModelDiscoverer = new ExtensionModelDiscoverer();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ExtensionManager create(MuleContext muleContext) {
    final ExtensionManager extensionManager = extensionManagerFactory.create(muleContext);
    final Set<ExtensionModel> extensions = new HashSet<>();
    extensionModelDiscoverer.discoverRuntimeExtensionModels()
        .forEach(extensionManager::registerExtension);
    extensions.addAll(extensionModelDiscoverer
        .discoverPluginsExtensionModels(extensionModelLoaderRepository, artifactPlugins)
        .stream().map(Pair::getSecond).collect(toSet()));
    extensions.forEach(extensionManager::registerExtension);
    return extensionManager;
  }

}
