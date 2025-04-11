/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.descriptor;

import static org.mule.runtime.module.artifact.activation.api.descriptor.DomainDescriptorResolver.noDomainDescriptorResolver;
import static org.mule.runtime.module.artifact.activation.api.plugin.PluginDescriptorResolver.pluginDescriptorResolver;
import static org.mule.runtime.module.artifact.activation.api.plugin.PluginModelResolver.pluginModelResolver;

import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.api.descriptor.DeployableArtifactDescriptorCreator;
import org.mule.runtime.module.artifact.activation.api.descriptor.DeployableArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.activation.api.descriptor.DomainDescriptorResolver;
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
                                                           PluginDescriptorResolver pluginDescriptorResolver,
                                                           DomainDescriptorResolver domainDescriptorResolver,
                                                           DeployableArtifactDescriptorCreator<ApplicationDescriptor> descriptorCreator) {
    model.validate();

    return new ApplicationDescriptorFactory(model, deploymentProperties, pluginModelResolver,
                                            pluginDescriptorResolver,
                                            ArtifactDescriptorValidatorBuilder.builder(), domainDescriptorResolver,
                                            descriptorCreator)
        .create();
  }

  @Override
  public ApplicationDescriptor createApplicationDescriptor(DeployableProjectModel model, Map<String, String> deploymentProperties,
                                                           PluginModelResolver pluginModelResolver,
                                                           PluginDescriptorResolver pluginDescriptorResolver) {
    return createApplicationDescriptor(model, deploymentProperties, pluginModelResolver,
                                       pluginDescriptorResolver,
                                       noDomainDescriptorResolver(),
                                       DeployableArtifactDescriptorCreator.applicationDescriptorCreator());
  }

  @Override
  public ApplicationDescriptor createApplicationDescriptor(DeployableProjectModel model,
                                                           Map<String, String> deploymentProperties,
                                                           DomainDescriptorResolver domainDescriptorResolver) {
    return createApplicationDescriptor(model, deploymentProperties, pluginModelResolver(),
                                       pluginDescriptorResolver(), domainDescriptorResolver,
                                       DeployableArtifactDescriptorCreator.applicationDescriptorCreator());
  }

  @Override
  public ApplicationDescriptor createApplicationDescriptor(DeployableProjectModel model,
                                                           Map<String, String> deploymentProperties,
                                                           DomainDescriptorResolver domainDescriptorResolver,
                                                           DeployableArtifactDescriptorCreator<ApplicationDescriptor> descriptorCreator) {
    return createApplicationDescriptor(model, deploymentProperties, pluginModelResolver(),
                                       pluginDescriptorResolver(), domainDescriptorResolver,
                                       descriptorCreator);
  }

  @Override
  public ApplicationDescriptor createApplicationDescriptor(DeployableProjectModel model,
                                                           Map<String, String> deploymentProperties) {
    return createApplicationDescriptor(model, deploymentProperties, noDomainDescriptorResolver(),
                                       DeployableArtifactDescriptorCreator.applicationDescriptorCreator());
  }

  @Override
  public DomainDescriptor createDomainDescriptor(DeployableProjectModel model,
                                                 Map<String, String> deploymentProperties,
                                                 PluginModelResolver pluginModelResolver,
                                                 PluginDescriptorResolver pluginDescriptorResolver,
                                                 DeployableArtifactDescriptorCreator<DomainDescriptor> descriptorCreator) {
    model.validate();

    return new DomainDescriptorFactory(model, deploymentProperties, pluginModelResolver,
                                       pluginDescriptorResolver,
                                       ArtifactDescriptorValidatorBuilder.builder(), descriptorCreator)
        .create();
  }

  @Override
  public DomainDescriptor createDomainDescriptor(DeployableProjectModel model, Map<String, String> deploymentProperties,
                                                 DeployableArtifactDescriptorCreator<DomainDescriptor> descriptorCreator) {
    return createDomainDescriptor(model, deploymentProperties, pluginModelResolver(),
                                  pluginDescriptorResolver(), descriptorCreator);
  }

  @Override
  public DomainDescriptor createDomainDescriptor(DeployableProjectModel model, Map<String, String> deploymentProperties) {
    return createDomainDescriptor(model, deploymentProperties, pluginModelResolver(),
                                  pluginDescriptorResolver(), DeployableArtifactDescriptorCreator.domainDescriptorCreator());
  }
}
