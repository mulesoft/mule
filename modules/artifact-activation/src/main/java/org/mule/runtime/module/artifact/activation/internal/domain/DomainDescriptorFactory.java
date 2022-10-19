/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.domain;

import static org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor.DEFAULT_CONFIGURATION_RESOURCE;

import static java.util.Collections.emptySet;

import org.mule.runtime.api.deployment.meta.MuleDomainModel;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginDescriptorResolver;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginModelResolver;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginPatchesResolver;
import org.mule.runtime.module.artifact.activation.internal.deployable.AbstractDeployableArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Creates an artifact descriptor for a domain.
 */
public class DomainDescriptorFactory extends AbstractDeployableArtifactDescriptorFactory<MuleDomainModel, DomainDescriptor> {

  public DomainDescriptorFactory(DeployableProjectModel deployableProjectModel,
                                 Map<String, String> deploymentProperties,
                                 PluginPatchesResolver artifactPatches,
                                 PluginModelResolver pluginModelResolver,
                                 PluginDescriptorResolver pluginDescriptorResolver,
                                 ArtifactDescriptorValidatorBuilder artifactDescriptorValidatorBuilder) {
    super(deployableProjectModel, deploymentProperties, artifactPatches, pluginModelResolver, pluginDescriptorResolver,
          artifactDescriptorValidatorBuilder);
  }

  @Override
  protected MuleDomainModel createArtifactModel() {
    return getDeployableModel();
  }

  @Override
  protected void doValidation(DomainDescriptor descriptor) {
    super.doValidation(descriptor);
    List<ArtifactPluginDescriptor> resolvedArtifactPluginDescriptors =
        getPluginDependenciesResolver().resolve(emptySet(), new ArrayList<>(descriptor.getPlugins()), true);

    // Refreshes the list of plugins on the descriptor with the resolved from transitive plugin dependencies
    descriptor.setPlugins(new LinkedHashSet<>(resolvedArtifactPluginDescriptors));
  }

  @Override
  protected String getDefaultConfigurationResource() {
    return DEFAULT_CONFIGURATION_RESOURCE;
  }

  @Override
  protected DomainDescriptor doCreateArtifactDescriptor() {
    return new DomainDescriptor(getArtifactLocation().getName(),
                                getDeploymentProperties());
  }
}
