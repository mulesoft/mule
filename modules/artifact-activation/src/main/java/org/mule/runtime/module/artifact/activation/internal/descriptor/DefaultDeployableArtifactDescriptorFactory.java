/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.descriptor;

import static org.mule.runtime.module.artifact.activation.api.descriptor.DomainDescriptorResolver.noDomainDescriptorResolver;
import static org.mule.runtime.module.artifact.activation.api.plugin.PluginDescriptorResolver.pluginDescriptorResolver;
import static org.mule.runtime.module.artifact.activation.api.plugin.PluginModelResolver.pluginModelResolver;
import static org.mule.runtime.module.artifact.activation.api.plugin.PluginPatchesResolver.pluginPatchesResolver;

import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.api.descriptor.DeployableArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.activation.api.descriptor.DomainDescriptorResolver;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginDescriptorResolver;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginModelResolver;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginPatchesResolver;
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
                                                           PluginPatchesResolver pluginPatchesResolver,
                                                           PluginModelResolver pluginModelResolver,
                                                           PluginDescriptorResolver pluginDescriptorResolver,
                                                           DomainDescriptorResolver domainDescriptorResolver) {
    model.validate();

    return new ApplicationDescriptorFactory(model, deploymentProperties, pluginPatchesResolver, pluginModelResolver,
                                            pluginDescriptorResolver,
                                            ArtifactDescriptorValidatorBuilder.builder(), domainDescriptorResolver).create();
  }

  @Override
  public ApplicationDescriptor createApplicationDescriptor(DeployableProjectModel model, Map<String, String> deploymentProperties,
                                                           PluginModelResolver pluginModelResolver,
                                                           PluginDescriptorResolver pluginDescriptorResolver) {
    return createApplicationDescriptor(model, deploymentProperties, pluginPatchesResolver(), pluginModelResolver,
                                       pluginDescriptorResolver,
                                       noDomainDescriptorResolver());
  }

  @Override
  public ApplicationDescriptor createApplicationDescriptor(DeployableProjectModel model,
                                                           Map<String, String> deploymentProperties,
                                                           DomainDescriptorResolver domainDescriptorResolver) {
    return createApplicationDescriptor(model, deploymentProperties, pluginPatchesResolver(), pluginModelResolver(),
                                       pluginDescriptorResolver(), domainDescriptorResolver);
  }

  @Override
  public ApplicationDescriptor createApplicationDescriptor(DeployableProjectModel model,
                                                           Map<String, String> deploymentProperties) {
    return createApplicationDescriptor(model, deploymentProperties, noDomainDescriptorResolver());
  }

  @Override
  public DomainDescriptor createDomainDescriptor(DeployableProjectModel model,
                                                 Map<String, String> deploymentProperties,
                                                 PluginPatchesResolver pluginPatchesResolver,
                                                 PluginModelResolver pluginModelResolver,
                                                 PluginDescriptorResolver pluginDescriptorResolver) {
    model.validate();

    return new DomainDescriptorFactory(model, deploymentProperties, pluginPatchesResolver, pluginModelResolver,
                                       pluginDescriptorResolver,
                                       ArtifactDescriptorValidatorBuilder.builder()).create();
  }

  @Override
  public DomainDescriptor createDomainDescriptor(DeployableProjectModel model, Map<String, String> deploymentProperties) {
    return createDomainDescriptor(model, deploymentProperties, pluginPatchesResolver(), pluginModelResolver(),
                                  pluginDescriptorResolver());
  }
}
