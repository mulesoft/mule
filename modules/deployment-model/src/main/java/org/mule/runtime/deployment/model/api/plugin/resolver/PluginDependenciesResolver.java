/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.deployment.model.api.plugin.resolver;

import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;

import java.util.List;
import java.util.Set;

/**
 * Resolves plugin dependencies to obtain a proper initialization order.
 * 
 * @since 4.5
 */
public interface PluginDependenciesResolver {

  /**
   * Resolves the dependencies between a group of plugins.
   *
   * @param providedPluginDescriptors plugins descriptors provided by a parent Mule artifact if it exists.
   * @param descriptors               plugins descriptors to resolve.
   * @param isDomain                  {@code true} if {@code providedPluginDescriptors} come from a {@code domain}, false
   *                                  otherwise.
   * @return a non null list containing the plugins in resolved order.
   * @throws PluginResolutionError if at least a plugin cannot be resolved.
   */
  List<ArtifactPluginDescriptor> resolve(Set<ArtifactPluginDescriptor> providedPluginDescriptors,
                                         List<ArtifactPluginDescriptor> descriptors, boolean isDomain)
      throws PluginResolutionError;
}
