/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
