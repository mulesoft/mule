/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.activation.internal.extension.discovery;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionDiscoveryRequest;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;

import java.util.Set;

/**
 * Implementations generate an {@link ExtensionModel} for a given artifact.
 *
 * @since 4.5
 */
public interface ExtensionModelGenerator {

  /**
   * Generate the {@link ExtensionModel} for the artifact with the given {@code artifactPluginDescriptor}.
   * 
   * @param discoveryRequest         the request that triggered the generation of this {@link ExtensionModel}.
   * @param artifactPluginDescriptor the descriptor of the plugin to generate the {@link ExtensionModel} for.
   * @param dependencies             the {@link ExtensionModel}s for the dependencies of {@code artifactPluginDescriptor}.
   * @return the generated {@link ExtensionModel} for {@code artifactPluginDescriptor}.
   */
  ExtensionModel obtainExtensionModel(ExtensionDiscoveryRequest discoveryRequest,
                                      ArtifactPluginDescriptor artifactPluginDescriptor,
                                      Set<ExtensionModel> dependencies);

}
