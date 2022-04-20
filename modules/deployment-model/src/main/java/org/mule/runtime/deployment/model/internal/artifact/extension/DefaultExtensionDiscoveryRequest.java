/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.artifact.extension;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.deployment.model.api.artifact.extension.ExtensionDiscoveryRequest;
import org.mule.runtime.deployment.model.api.artifact.extension.ExtensionModelLoaderRepository;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;

import java.util.List;
import java.util.Set;

@Deprecated
// TODO W-10928152: remove this class when migrating to use the new extension model loading API.
public class DefaultExtensionDiscoveryRequest implements ExtensionDiscoveryRequest {

  private final ExtensionModelLoaderRepository loaderRepository;
  private final List<Pair<ArtifactPluginDescriptor, ArtifactClassLoader>> artifactPlugins;
  private final Set<ExtensionModel> parentArtifactExtensions;
  private final boolean parallelDiscovery;
  private final boolean enrichDescriptions;

  public DefaultExtensionDiscoveryRequest(ExtensionModelLoaderRepository loaderRepository,
                                          List<Pair<ArtifactPluginDescriptor, ArtifactClassLoader>> artifactPlugins,
                                          Set<ExtensionModel> parentArtifactExtensions,
                                          boolean parallelDiscovery,
                                          boolean enrichDescriptions) {
    this.loaderRepository = loaderRepository;
    this.artifactPlugins = artifactPlugins;
    this.parentArtifactExtensions = parentArtifactExtensions;
    this.parallelDiscovery = parallelDiscovery;
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

  @Override
  public boolean isParallelDiscovery() {
    return parallelDiscovery;
  }

  @Override
  public boolean isEnrichDescriptions() {
    return enrichDescriptions;
  }

}
