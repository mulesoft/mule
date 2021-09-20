/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.artifact.extension;

import static java.util.Collections.emptySet;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.internal.artifact.extension.DefaultExtensionDiscoveryCommand;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;

import java.util.List;
import java.util.Set;

public interface ExtensionDiscoveryCommand {

  static ExtensionDiscoveryCommandBuilder builder() {
    return new ExtensionDiscoveryCommandBuilder();
  }

  /**
   * @return {@link ExtensionModelLoaderRepository} with the available extension loaders.
   */
  ExtensionModelLoaderRepository getLoaderRepository();

  /**
   * @return {@link Pair} of {@link ArtifactPluginDescriptor} and {@link ArtifactClassLoader} for artifact plugins deployed inside
   *         the artifact. Non null.
   */
  List<Pair<ArtifactPluginDescriptor, ArtifactClassLoader>> getArtifactPlugins();

  /**
   * @return {@link Set} of {@link ExtensionModel} to also take into account when parsing extensions
   */
  Set<ExtensionModel> getParentArtifactExtensions();

  /**
   * @return {@code true} if any {@link DeclarationEnricher} that adds descriptions to a {@link ExtensionDeclaration} must be
   *         executed, {@code false} it if must be skipped.
   */
  boolean isEnrichDescriptions();

  public final class ExtensionDiscoveryCommandBuilder {

    private ExtensionModelLoaderRepository loaderRepository;
    private List<Pair<ArtifactPluginDescriptor, ArtifactClassLoader>> artifactPlugins;
    private Set<ExtensionModel> parentArtifactExtensions = emptySet();
    private boolean enrichDescriptions = true;

    public ExtensionDiscoveryCommandBuilder setLoaderRepository(ExtensionModelLoaderRepository loaderRepository) {
      this.loaderRepository = loaderRepository;
      return this;
    }

    public ExtensionDiscoveryCommandBuilder setArtifactPlugins(List<Pair<ArtifactPluginDescriptor, ArtifactClassLoader>> artifactPlugins) {
      this.artifactPlugins = artifactPlugins;
      return this;
    }

    public ExtensionDiscoveryCommandBuilder setParentArtifactExtensions(Set<ExtensionModel> parentArtifactExtensions) {
      this.parentArtifactExtensions = parentArtifactExtensions;
      return this;
    }

    public ExtensionDiscoveryCommandBuilder setEnrichDescriptions(boolean enrichDescriptions) {
      this.enrichDescriptions = enrichDescriptions;
      return this;
    }

    public ExtensionDiscoveryCommand build() {
      return new DefaultExtensionDiscoveryCommand(loaderRepository, artifactPlugins, parentArtifactExtensions,
                                                  enrichDescriptions);
    }
  }
}
