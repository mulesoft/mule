/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.plugin;

import static java.util.Collections.emptyList;

import org.mule.tools.api.classloader.model.ArtifactCoordinates;

import java.net.URL;
import java.util.List;

/**
 * Implementation of {@link PluginPatchesResolver} which always returns an empty list
 *
 * @since 4.5
 */
class NullPluginPatchesResolver implements PluginPatchesResolver {

  @Override
  public List<URL> resolve(ArtifactCoordinates pluginArtifactCoordinates) {
    return emptyList();
  }
}
