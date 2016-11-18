/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.plugin;

import static java.util.Optional.*;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ArtifactPluginDescriptor extends DeployableArtifactDescriptor {

  public static final String MULE_PLUGIN_CLASSIFIER = "mule-plugin";
  public static final String EXTENSION_BUNDLE_TYPE = "zip";
  public static final String PLUGIN_PROPERTIES = "plugin.properties";
  public static final String META_INF = "META-INF";
  public static final String MULE_PLUGIN_JSON = "mule-plugin.json";

  private List<ArtifactPluginDescriptor> artifactPluginDescriptors = new ArrayList<>();
  private Optional<LoaderDescriber> extensionModelDescriptorProperty = empty();

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

  /**
   * @return the {@link LoaderDescriber} that will contain all mandatory values to generate an {@link ExtensionModel}
   * from it.
   */
  public Optional<LoaderDescriber> getExtensionModelDescriptorProperty() {
    return extensionModelDescriptorProperty;
  }

  /**
   * Takes a {@link LoaderDescriber} that should contain the values used to properly initialize an {@link ExtensionModel}
   *
   * @param extensionModelLoaderDescriber the {@link LoaderDescriber} with the values
   */
  public void setExtensionModelDescriptorProperty(LoaderDescriber extensionModelLoaderDescriber) {
    this.extensionModelDescriptorProperty = ofNullable(extensionModelLoaderDescriber);
  }
}
