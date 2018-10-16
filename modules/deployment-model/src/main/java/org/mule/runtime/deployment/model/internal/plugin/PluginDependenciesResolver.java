/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.internal.plugin;

import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;

import java.util.List;
import java.util.Set;

/**
 * Resolves resolves plugin dependencies to obtain a proper initialization order.
 */
public interface PluginDependenciesResolver {

  /**
   * Resolves the dependencies between a group of plugins.
   *
   * @param providedPluginDescriptors plugins descriptors provided by a parent Mule artifact if it exists.
   * @param descriptors plugins descriptors to resolve.
   * @param isDomain
   * @return a non null list containing the plugins in resolved order.
   * @throws PluginResolutionError if at least a plugin cannot be resolved.
   */
  List<ArtifactPluginDescriptor> resolve(Set<ArtifactPluginDescriptor> providedPluginDescriptors,
                                         List<ArtifactPluginDescriptor> descriptors, boolean isDomain);
}
