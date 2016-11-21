/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.plugin;

import org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor;

import java.util.ArrayList;
import java.util.List;

public class ArtifactPluginDescriptor extends DeployableArtifactDescriptor {

  public static final String MULE_PLUGIN_CLASSIFIER = "mule-plugin";
  public static final String EXTENSION_BUNDLE_TYPE = "zip";
  public static final String PLUGIN_PROPERTIES = "plugin.properties";

  private List<ArtifactPluginDescriptor> artifactPluginDescriptors = new ArrayList<>();

  /**
   * Creates a new artifact plugin descriptor
   *
   * @param name artifact plugin name. Non empty.
   */
  public ArtifactPluginDescriptor(String name) {
    super(name);
  }

  public List<ArtifactPluginDescriptor> getArtifactPluginDescriptors() {
    return artifactPluginDescriptors;
  }

  public void setArtifactPluginDescriptors(List<ArtifactPluginDescriptor> pluginDependencyDescriptors) {
    this.artifactPluginDescriptors = pluginDependencyDescriptors;
  }
}
