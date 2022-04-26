/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.extension.discovery;

import static java.util.Collections.emptySet;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.module.artifact.activation.internal.extension.discovery.DefaultExtensionDiscoveryRequest;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;

import java.util.List;
import java.util.Set;

/**
 * Container of the parameters that can be given to
 * {@link ExtensionModelDiscoverer#discoverPluginsExtensionModels(ExtensionDiscoveryRequest)}.
 * 
 * @since 4.5
 */
public interface ExtensionDiscoveryRequest {

  /**
   * @return a fluent builder for creating a new {@link ExtensionDiscoveryRequest} instance.
   */
  static ExtensionDiscoveryRequestBuilder builder() {
    return new ExtensionDiscoveryRequestBuilder();
  }

  /**
   * @return {@link ArtifactPluginDescriptor}s for artifact plugins deployed inside the artifact. Non-null.
   */
  List<ArtifactPluginDescriptor> getArtifactPluginDescriptors();

  /**
   * @return {@link Set} of {@link ExtensionModel} to also take into account when parsing extensions
   */
  Set<ExtensionModel> getParentArtifactExtensions();

  /**
   * Parallel discovery will try to parallelize only the discovery for extensions that do not depend on the DSL of other
   * extensions.
   * <p>
   * Parallelism is achieved using the {@code fork-join} pool.
   * 
   * @return {@code true} if the extension model discovery process will attempt to discover an extension model from the
   *         classloaders in parallel instead of sequentially.
   */
  boolean isParallelDiscovery();

  /**
   * @return {@code true} if any {@link DeclarationEnricher} that adds descriptions to an {@link ExtensionDeclaration} must be
   *         executed, {@code false} if it must be skipped.
   */
  boolean isEnrichDescriptions();

  final class ExtensionDiscoveryRequestBuilder {

    private List<ArtifactPluginDescriptor> artifactPlugins;
    private Set<ExtensionModel> parentArtifactExtensions = emptySet();
    private boolean parallelDiscovery = false;
    private boolean enrichDescriptions = true;

    public ExtensionDiscoveryRequestBuilder setArtifactPlugins(List<ArtifactPluginDescriptor> artifactPlugins) {
      this.artifactPlugins = artifactPlugins;
      return this;
    }

    public ExtensionDiscoveryRequestBuilder setParentArtifactExtensions(Set<ExtensionModel> parentArtifactExtensions) {
      this.parentArtifactExtensions = parentArtifactExtensions;
      return this;
    }

    public ExtensionDiscoveryRequestBuilder setEnrichDescriptions(boolean enrichDescriptions) {
      this.enrichDescriptions = enrichDescriptions;
      return this;
    }

    public ExtensionDiscoveryRequestBuilder setParallelDiscovery(boolean parallelDiscovery) {
      this.parallelDiscovery = parallelDiscovery;
      return this;
    }

    public ExtensionDiscoveryRequest build() {
      return new DefaultExtensionDiscoveryRequest(artifactPlugins, parentArtifactExtensions,
                                                  parallelDiscovery, enrichDescriptions);
    }
  }
}
