/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.policy;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.util.Pair;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.deployment.impl.internal.artifact.ExtensionModelDiscoverer;
import org.mule.runtime.module.extension.internal.loader.ExtensionModelLoaderRepository;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerFactory;

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
    extensions.addAll(extensionModelDiscoverer.discoverExtensionModels(extensionModelLoaderRepository, artifactPlugins));
    extensions.forEach(extensionManager::registerExtension);
    return extensionManager;
  }

}
