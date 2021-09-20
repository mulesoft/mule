/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.artifact.extension;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.deployment.model.api.artifact.extension.ExtensionDiscoveryCommand;
import org.mule.runtime.deployment.model.api.artifact.extension.ExtensionModelLoaderRepository;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;

import java.util.List;
import java.util.Set;

public class DefaultExtensionDiscoveryCommand implements ExtensionDiscoveryCommand {

  private final ExtensionModelLoaderRepository loaderRepository;
  private final List<Pair<ArtifactPluginDescriptor, ArtifactClassLoader>> artifactPlugins;
  private final Set<ExtensionModel> parentArtifactExtensions;
  private final boolean enrichDescriptions;

  public DefaultExtensionDiscoveryCommand(ExtensionModelLoaderRepository loaderRepository,
                                          List<Pair<ArtifactPluginDescriptor, ArtifactClassLoader>> artifactPlugins,
                                          Set<ExtensionModel> parentArtifactExtensions, boolean enrichDescriptions) {
    this.loaderRepository = loaderRepository;
    this.artifactPlugins = artifactPlugins;
    this.parentArtifactExtensions = parentArtifactExtensions;
    this.enrichDescriptions = enrichDescriptions;
  }

  @Override
  public ExtensionModelLoaderRepository getLoaderRepository() {
    return loaderRepository;
  }

  @Override
  public List<Pair<ArtifactPluginDescriptor, ArtifactClassLoader>> getArtifactPlugins() {
    return artifactPlugins;
  }

  @Override
  public Set<ExtensionModel> getParentArtifactExtensions() {
    return parentArtifactExtensions;
  }

  public boolean isEnrichDescriptions() {
    return enrichDescriptions;
  }

}
