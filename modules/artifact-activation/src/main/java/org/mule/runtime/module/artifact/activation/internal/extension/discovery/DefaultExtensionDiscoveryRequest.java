/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.extension.discovery;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.internal.util.collection.UnmodifiableMap.unmodifiableMap;

import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionDiscoveryRequest;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelDiscoverer;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Default implementation of {@link ExtensionDiscoveryRequest}, which contains the parameters that can be given to
 * {@link ExtensionModelDiscoverer#discoverPluginsExtensionModels(ExtensionDiscoveryRequest)}
 *
 * @since 4.5
 */
public class DefaultExtensionDiscoveryRequest implements ExtensionDiscoveryRequest {

  private final Collection<ArtifactPluginDescriptor> artifactPlugins;
  private final Set<ExtensionModel> parentArtifactExtensions;
  private final boolean parallelDiscovery;
  private final boolean enrichDescriptions;
  private final boolean ocsEnabled;
  private final Map<String, Object> customParameters;

  public DefaultExtensionDiscoveryRequest(Collection<ArtifactPluginDescriptor> artifactPlugins,
                                          Set<ExtensionModel> parentArtifactExtensions,
                                          boolean parallelDiscovery,
                                          boolean enrichDescriptions) {
    this(artifactPlugins, parentArtifactExtensions, parallelDiscovery, enrichDescriptions, false, emptyMap());
  }

  public DefaultExtensionDiscoveryRequest(Collection<ArtifactPluginDescriptor> artifactPlugins,
                                          Set<ExtensionModel> parentArtifactExtensions, boolean parallelDiscovery,
                                          boolean enrichDescriptions, boolean ocsEnabled, Map<String, Object> customParameters) {
    this.artifactPlugins = artifactPlugins;
    this.parentArtifactExtensions = parentArtifactExtensions;
    this.parallelDiscovery = parallelDiscovery;
    this.enrichDescriptions = enrichDescriptions;
    this.ocsEnabled = ocsEnabled;
    this.customParameters = unmodifiableMap(customParameters);
  }

  @Override
  public <T> Optional<T> getParameter(String key) {
    return ofNullable((T) customParameters.get(key));
  }

  @Override
  public Map<String, Object> getParameters() {
    return customParameters;
  }

  @Override
  public Collection<ArtifactPluginDescriptor> getArtifactPluginDescriptors() {
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

  @Override
  public boolean isOCSEnabled() {
    return ocsEnabled;
  }

}
