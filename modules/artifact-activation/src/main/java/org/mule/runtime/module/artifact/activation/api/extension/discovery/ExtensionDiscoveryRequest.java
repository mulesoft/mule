/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.extension.discovery;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import static java.util.Collections.emptySet;

import org.mule.api.annotation.NoImplement;
import org.mule.api.annotation.NoInstantiate;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.version.HasMinMuleVersion;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.module.artifact.activation.internal.extension.discovery.DefaultExtensionDiscoveryRequest;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Container of the parameters that can be given to
 * {@link ExtensionModelDiscoverer#discoverPluginsExtensionModels(ExtensionDiscoveryRequest)}.
 *
 * @since 4.5
 */
@NoImplement
public interface ExtensionDiscoveryRequest {

  /**
   * @return a fluent builder for creating a new {@link ExtensionDiscoveryRequest} instance.
   */
  static ExtensionDiscoveryRequestBuilder builder() {
    return new ExtensionDiscoveryRequestBuilder();
  }

  /**
   * Obtains the custom parameter registered under {@code key}.
   *
   * @param key the key under which the wanted value is registered.
   * @param <T> generic type of the expected value.
   * @return an {@link Optional} value.
   */
  <T> Optional<T> getParameter(String key);

  /**
   * Returns all the parameters for this discovery request.
   *
   * @return parameters for this discovery request.
   */
  Map<String, Object> getParameters();

  /**
   * @return {@link ArtifactPluginDescriptor}s for artifact plugins deployed inside the artifact. Non-null.
   */
  Collection<ArtifactPluginDescriptor> getArtifactPluginDescriptors();

  /**
   * @return {@link Set} of {@link ExtensionModel} to also take into account when parsing extensions.
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

  /**
   * @return whether OCS is enabled.
   */
  boolean isOCSEnabled();

  /**
   * @return whether the validation must be validated after being loaded.
   */
  boolean isForceExtensionValidation();

  /**
   * @return whether the {@link HasMinMuleVersion#getMinMuleVersion() minMuleVersion} of each component must be calculated.
   * @since 1.9
   */
  boolean isResolveMinMuleVersion();

  @NoInstantiate
  final class ExtensionDiscoveryRequestBuilder {

    private Collection<ArtifactPluginDescriptor> artifactPlugins;
    private Set<ExtensionModel> parentArtifactExtensions = emptySet();
    private boolean parallelDiscovery = false;
    private boolean enrichDescriptions = true;
    private boolean ocsEnabled = false;
    private boolean forceExtensionValidation = false;
    private boolean resolveMinMuleVersion = false;
    private final Map<String, Object> customParameters = new HashMap<>();

    public ExtensionDiscoveryRequestBuilder setArtifactPlugins(Collection<ArtifactPluginDescriptor> artifactPlugins) {
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

    public ExtensionDiscoveryRequestBuilder setOCSEnabled(boolean ocsEnabled) {
      this.ocsEnabled = ocsEnabled;
      return this;
    }

    public ExtensionDiscoveryRequestBuilder setForceExtensionValidation(boolean forceExtensionValidation) {
      this.forceExtensionValidation = forceExtensionValidation;
      return this;
    }


    public ExtensionDiscoveryRequestBuilder setResolveMinMuleVersion(boolean resolveMinMuleVersion) {
      this.resolveMinMuleVersion = resolveMinMuleVersion;
      return this;
    }

    /**
     * Adds a custom parameter registered under {@code key}.
     *
     * @param key   the key under which the {@code value} is to be registered.
     * @param value the custom parameter value.
     * @throws IllegalArgumentException if {@code key} or {@code value} are {@code null}.
     */
    public ExtensionDiscoveryRequestBuilder addParameter(String key, Object value) {
      checkArgument(key != null && key.length() > 0, "key cannot be blank");
      checkArgument(value != null, "value cannot be null");

      customParameters.put(key, value);
      return this;
    }

    /**
     * Adds the contents of the given map as custom parameters.
     *
     * @param parameters a map with custom parameters.
     */
    public ExtensionDiscoveryRequestBuilder addParameters(Map<String, Object> parameters) {
      checkArgument(parameters != null, "cannot add null parameters");

      parameters.forEach(this::addParameter);
      return this;
    }

    public ExtensionDiscoveryRequest build() {
      return new DefaultExtensionDiscoveryRequest(artifactPlugins,
                                                  parentArtifactExtensions,
                                                  parallelDiscovery,
                                                  enrichDescriptions,
                                                  ocsEnabled,
                                                  forceExtensionValidation,
                                                  resolveMinMuleVersion,
                                                  customParameters);
    }
  }
}
