/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.extension;

import org.mule.runtime.api.meta.model.ExtensionModel;
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
