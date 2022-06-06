/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.descriptor;

import static org.mule.runtime.module.artifact.activation.api.plugin.PluginDescriptorResolver.pluginDescriptorResolver;
import static org.mule.runtime.module.artifact.activation.api.plugin.PluginModelResolver.pluginModelResolver;

import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.api.descriptor.DeployableArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginDescriptorResolver;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginModelResolver;
import org.mule.runtime.module.artifact.activation.internal.application.ApplicationDescriptorFactory;
import org.mule.runtime.module.artifact.activation.internal.domain.DomainDescriptorFactory;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;

import java.util.Map;

/**
 * Default implementation of {@link DeployableArtifactDescriptorFactory}.
 *
 * @since 4.5
 */
public class DefaultDeployableArtifactDescriptorFactory implements DeployableArtifactDescriptorFactory {

  @Override
  public ApplicationDescriptor createApplicationDescriptor(DeployableProjectModel model,
                                                           Map<String, String> deploymentProperties,
                                                           PluginModelResolver pluginModelResolver,
                                                           PluginDescriptorResolver pluginDescriptorResolver) {

    return new ApplicationDescriptorFactory(model, deploymentProperties, pluginModelResolver, pluginDescriptorResolver,
                                            ArtifactDescriptorValidatorBuilder.builder()).create();
  }

  @Override
  public ApplicationDescriptor createApplicationDescriptor(DeployableProjectModel model,
                                                           Map<String, String> deploymentProperties) {
    return createApplicationDescriptor(model, deploymentProperties,
                                       pluginModelResolver(),
                                       pluginDescriptorResolver());
  }

  @Override
  public DomainDescriptor createDomainDescriptor(DeployableProjectModel model,
                                                 Map<String, String> deploymentProperties,
                                                 PluginModelResolver pluginModelResolver,
                                                 PluginDescriptorResolver pluginDescriptorResolver) {
    return new DomainDescriptorFactory(model, deploymentProperties, pluginModelResolver, pluginDescriptorResolver,
                                       ArtifactDescriptorValidatorBuilder.builder()).create();
  }

  @Override
  public DomainDescriptor createDomainDescriptor(DeployableProjectModel model, Map<String, String> deploymentProperties) {
    return createDomainDescriptor(model, deploymentProperties,
                                  pluginModelResolver(),
                                  pluginDescriptorResolver());
  }
}
