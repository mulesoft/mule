/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.application;

import static org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor.DEFAULT_CONFIGURATION_RESOURCE;

import org.mule.runtime.api.deployment.meta.MuleApplicationModel;
import org.mule.runtime.api.deployment.persistence.AbstractMuleArtifactModelJsonSerializer;
import org.mule.runtime.api.deployment.persistence.MuleApplicationModelJsonSerializer;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginDescriptorResolver;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginModelResolver;
import org.mule.runtime.module.artifact.activation.internal.deployable.AbstractDeployableArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder;

import java.util.Map;

/**
 * Creates an artifact descriptor for an application.
 */
public class ApplicationDescriptorFactory
    extends AbstractDeployableArtifactDescriptorFactory<MuleApplicationModel, ApplicationDescriptor> {

  public ApplicationDescriptorFactory(DeployableProjectModel deployableProjectModel,
                                      Map<String, String> deploymentProperties, PluginModelResolver pluginModelResolver,
                                      PluginDescriptorResolver pluginDescriptorResolver,
                                      ArtifactDescriptorValidatorBuilder artifactDescriptorValidatorBuilder) {
    super(deployableProjectModel, deploymentProperties, pluginModelResolver, pluginDescriptorResolver,
          artifactDescriptorValidatorBuilder);
  }

  @Override
  protected AbstractMuleArtifactModelJsonSerializer<MuleApplicationModel> getMuleArtifactModelJsonSerializer() {
    return new MuleApplicationModelJsonSerializer();
  }

  @Override
  protected void doDescriptorConfig(ApplicationDescriptor descriptor) {
    super.doDescriptorConfig(descriptor);
    getArtifactModel().getDomain().ifPresent(descriptor::setDomainName);
  }

  @Override
  protected String getDefaultConfigurationResource() {
    return DEFAULT_CONFIGURATION_RESOURCE;
  }

  @Override
  protected ApplicationDescriptor doCreateArtifactDescriptor() {
    return new ApplicationDescriptor(getArtifactLocation().getName(), getDeploymentProperties());
  }
}
