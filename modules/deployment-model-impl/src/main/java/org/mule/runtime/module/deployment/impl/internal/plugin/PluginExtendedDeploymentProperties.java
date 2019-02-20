/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.plugin;

import static com.google.common.base.Preconditions.checkNotNull;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

import java.util.Properties;

/**
 * Extends deployment properties to provide more context when resolving the {@link org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel} for
 * plugins.
 *
 * @since 4.1.5
 */
public class PluginExtendedDeploymentProperties extends ExtendedDeploymentProperties {

  private final BundleDescriptor pluginBundleDescriptor;
  private final ArtifactDescriptor deployableArtifactDescriptor;

  /**
   * Plugin extended deployment properties which also provides the {@link ArtifactDescriptor} where the plugin has
   * been declared as dependency.
   *
   * @param deploymentProperties the original properties passed from deployment service. Can be null.
   * @param pluginBundleDescriptor the plugin bundle descriptor resolved from the deployable artifact descriptor. Not null.
   * @param deployableArtifactDescriptor the container in which the plugin is declared as dependency. Not null.
   */
  public PluginExtendedDeploymentProperties(Properties deploymentProperties,
                                            BundleDescriptor pluginBundleDescriptor,
                                            ArtifactDescriptor deployableArtifactDescriptor) {
    super(deploymentProperties);
    checkNotNull(pluginBundleDescriptor, "pluginBundleDescriptor cannot be null");
    checkNotNull(deployableArtifactDescriptor, "deployableArtifactDescriptor cannot be null");
    this.pluginBundleDescriptor = pluginBundleDescriptor;
    this.deployableArtifactDescriptor = deployableArtifactDescriptor;
  }

  /**
   * @return {@link BundleDescriptor} as resolved from the deployable artifact, this allows to use timestamp snapshot versions.
   */
  public BundleDescriptor getPluginBundleDescriptor() {
    return pluginBundleDescriptor;
  }

  /**
   * @return {@link ArtifactDescriptor} in which the plugin is declared as dependency.
   */
  public ArtifactDescriptor getDeployableArtifactDescriptor() {
    return deployableArtifactDescriptor;
  }

}
