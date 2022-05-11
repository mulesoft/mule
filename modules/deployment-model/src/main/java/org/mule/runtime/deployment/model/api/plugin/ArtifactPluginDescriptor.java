/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.plugin;

import org.mule.runtime.api.meta.model.ExtensionModel;

import java.util.Optional;
import java.util.Properties;

/**
 * @deprecated since 4.5 use org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor instead.
 */
@Deprecated
public final class ArtifactPluginDescriptor extends org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor {

  /**
   * Creates a new artifact plugin descriptor
   *
   * @param name artifact plugin name. Non empty.
   */
  public ArtifactPluginDescriptor(String name) {
    super(name);
  }

  /**
   * Creates a new artifact plugin descriptor
   *
   * @param name                 artifact plugin name. Non empty.
   * @param deploymentProperties deployment properties provided for the plugin
   */
  public ArtifactPluginDescriptor(String name, Optional<Properties> deploymentProperties) {
    super(name, deploymentProperties);
  }

  /**
   * Takes a {@link LoaderDescriber} that should contain the values used to properly initialize an {@link ExtensionModel}
   *
   * @param extensionModelLoaderDescriber the {@link LoaderDescriber} with the values
   */
  public void setExtensionModelDescriptorProperty(LoaderDescriber extensionModelLoaderDescriber) {
    super.setExtensionModelDescriptorProperty(extensionModelLoaderDescriber);
  }

}
