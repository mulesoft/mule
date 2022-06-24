/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.plugin;

import static java.util.Collections.emptyList;

import org.mule.runtime.module.artifact.activation.internal.plugin.DefaultPluginPatchesResolver;
import org.mule.tools.api.classloader.model.ArtifactCoordinates;

import java.net.URL;
import java.util.List;

/**
 * Resolves the patches that must be applied to a given plugin.
 *
 * @since 4.5
 */
// TODO W-11086310 - move out of this module as part of the migration of the Runtime to use the descriptors generation API
public interface PluginPatchesResolver {

  static PluginPatchesResolver noOpPluginPatchesResolver() {
    return pluginArtifactCoordinates -> emptyList();
  }

  /**
   * @return the default implementation of a {@link PluginPatchesResolver}, which resolves plugin patches for a standalone Mule
   *         Runtime.
   */
  static PluginPatchesResolver pluginPatchesResolver() {
    return new DefaultPluginPatchesResolver();
  }

  /**
   * @param pluginArtifactCoordinates artifact coordinates of the plugin to resolve patches for.
   * @return a {@link List} of {@link URL}s indicating the location of the patches that apply to the plugin.
   */
  List<URL> resolve(ArtifactCoordinates pluginArtifactCoordinates);
}
