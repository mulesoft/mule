/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.domain;

import static org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor.DEFAULT_CONFIGURATION_RESOURCE;

import org.mule.runtime.api.deployment.meta.MuleDomainModel;
import org.mule.runtime.api.deployment.persistence.AbstractMuleArtifactModelJsonSerializer;
import org.mule.runtime.api.deployment.persistence.MuleDomainModelJsonSerializer;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginDescriptorResolver;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginModelResolver;
import org.mule.runtime.module.artifact.activation.internal.deployable.AbstractDeployableArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;

import java.util.Map;

/**
 * Creates an artifact descriptor for a domain.
 */
public class DomainDescriptorFactory extends AbstractDeployableArtifactDescriptorFactory<MuleDomainModel, DomainDescriptor> {

  public DomainDescriptorFactory(DeployableProjectModel deployableProjectModel,
                                 Map<String, String> deploymentProperties, PluginModelResolver pluginModelResolver,
                                 PluginDescriptorResolver pluginDescriptorResolver,
                                 ArtifactDescriptorValidatorBuilder artifactDescriptorValidatorBuilder) {
    super(deployableProjectModel, deploymentProperties, pluginModelResolver, pluginDescriptorResolver,
          artifactDescriptorValidatorBuilder);
  }

  @Override
  protected AbstractMuleArtifactModelJsonSerializer<MuleDomainModel> getMuleArtifactModelJsonSerializer() {
    return new MuleDomainModelJsonSerializer();
  }

  @Override
  protected String getDefaultConfigurationResource() {
    return DEFAULT_CONFIGURATION_RESOURCE;
  }

  @Override
  protected DomainDescriptor doCreateArtifactDescriptor() {
    return new DomainDescriptor(getArtifactLocation().getName(), getDeploymentProperties());
  }
}
