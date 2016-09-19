/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.internal.plugin;

import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;

import java.util.List;

/**
 * Resolves resolves plugin dependencies to obtain a proper initialization order.
 */
public interface PluginDependenciesResolver {

  /**
   * Resolves the dependencies between a group of plugins.
   *
   * @param descriptors plugins descriptors to resolve.
   * @return a non null list containing the plugins in resolved order.
   * @throws PluginResolutionError if at least a plugin cannot be resolved.
   */
  List<ArtifactPluginDescriptor> resolve(List<ArtifactPluginDescriptor> descriptors);
}
