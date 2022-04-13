/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.extension;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.module.artifact.activation.api.extension.ExtensionDiscoveryRequest;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;

import java.util.List;
import java.util.Set;

public class DefaultExtensionDiscoveryRequest implements ExtensionDiscoveryRequest {

  private final List<ArtifactPluginDescriptor> artifactPlugins;
  private final Set<ExtensionModel> parentArtifactExtensions;
  private final boolean parallelDiscovery;
  private final boolean enrichDescriptions;

  public DefaultExtensionDiscoveryRequest(List<ArtifactPluginDescriptor> artifactPlugins,
                                          Set<ExtensionModel> parentArtifactExtensions,
                                          boolean parallelDiscovery,
                                          boolean enrichDescriptions) {
    this.artifactPlugins = artifactPlugins;
    this.parentArtifactExtensions = parentArtifactExtensions;
    this.parallelDiscovery = parallelDiscovery;
    this.enrichDescriptions = enrichDescriptions;
  }

  @Override
  public List<ArtifactPluginDescriptor> getArtifactPlugins() {
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
