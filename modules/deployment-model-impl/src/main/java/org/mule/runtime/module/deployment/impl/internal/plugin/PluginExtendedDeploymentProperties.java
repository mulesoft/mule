/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.plugin;

import static com.google.common.base.Preconditions.checkNotNull;
import org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor;

import java.util.Properties;

/**
 * Extends deployment properties to provide more context when resolving the {@link org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel} for
 * plugins.
 *
 * @since 4.2.0
 */
public class PluginExtendedDeploymentProperties extends ExtendedDeploymentProperties {

  private final DeployableArtifactDescriptor deployableArtifactDescriptor;

  /**
   * Plugin extended deployment properties which also provides the {@link DeployableArtifactDescriptor} where the plugin has
   * been declared as dependency.
   *
   * @param deploymentProperties the original properties passed from deployment service. Can be null.
   * @param deployableArtifactDescriptor the container in which the plugin is declared as dependency. Not null.
   */
  public PluginExtendedDeploymentProperties(Properties deploymentProperties,
                                            DeployableArtifactDescriptor deployableArtifactDescriptor) {
    super(deploymentProperties);
    checkNotNull(deployableArtifactDescriptor, "deployableArtifactDescriptor cannot be null");
    this.deployableArtifactDescriptor = deployableArtifactDescriptor;
  }

  /**
   * @return {@link DeployableArtifactDescriptor} in which the plugin is declared as dependency.
   */
  public DeployableArtifactDescriptor getDeployableArtifactDescriptor() {
    return deployableArtifactDescriptor;
  }

}
