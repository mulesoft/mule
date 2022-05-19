/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.descriptor;

import org.mule.runtime.api.deployment.meta.MuleApplicationModel;
import org.mule.runtime.api.deployment.meta.MuleDomainModel;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.module.artifact.activation.api.descriptor.ArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginDescriptorResolver;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginModelResolver;
import org.mule.runtime.module.artifact.activation.internal.application.ApplicationDescriptorFactory;
import org.mule.runtime.module.artifact.activation.internal.domain.DomainDescriptorFactory;
import org.mule.runtime.module.artifact.activation.internal.plugin.ArtifactPluginDescriptorFactory;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.DeployableArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;
import org.mule.tools.api.classloader.model.Artifact;
import org.mule.tools.api.classloader.model.ArtifactCoordinates;

import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link ArtifactDescriptorFactory}.
 *
 * @since 4.5
 */
public class DefaultArtifactDescriptorFactory implements ArtifactDescriptorFactory {

  @Override
  public ApplicationDescriptor createApplicationDescriptor(DeployableProjectModel<MuleApplicationModel> model,
                                                           Map<String, String> deploymentProperties,
                                                           PluginModelResolver pluginModelResolver,
                                                           PluginDescriptorResolver pluginDescriptorResolver) {

    return new ApplicationDescriptorFactory(model, deploymentProperties, pluginModelResolver, pluginDescriptorResolver,
                                            ArtifactDescriptorValidatorBuilder.builder(), this).createArtifactDescriptor();
  }

  @Override
  public DomainDescriptor createDomainDescriptor(DeployableProjectModel<MuleDomainModel> model,
                                                 Map<String, String> deploymentProperties,
                                                 PluginModelResolver pluginModelResolver,
                                                 PluginDescriptorResolver pluginDescriptorResolver) {
    return new DomainDescriptorFactory(model, deploymentProperties, pluginModelResolver, pluginDescriptorResolver,
                                       ArtifactDescriptorValidatorBuilder.builder(), this).createArtifactDescriptor();
  }

  @Override
  public ArtifactPluginDescriptor createPluginDescriptor(BundleDependency bundleDependency,
                                                         MulePluginModel pluginModel,
                                                         DeployableArtifactDescriptor ownerDescriptor,
                                                         List<BundleDependency> bundleDependencies,
                                                         ArtifactCoordinates pluginArtifactCoordinates,
                                                         List<Artifact> pluginDependencies,
                                                         List<String> pluginExportedPackages,
                                                         List<String> pluginExportedResources) {
    return new ArtifactPluginDescriptorFactory(bundleDependency, pluginModel, ownerDescriptor,
                                               bundleDependencies, pluginArtifactCoordinates, pluginDependencies,
                                               pluginExportedPackages, pluginExportedResources,
                                               ArtifactDescriptorValidatorBuilder.builder()).createArtifactDescriptor();
  }

}
