/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.maven;

import org.mule.maven.client.api.model.MavenConfiguration;
import org.mule.runtime.api.artifact.ArtifactCoordinates;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.api.deployable.MuleProjectStructure;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class MavenRefreshDeployableProjectModelBuilder extends MavenDeployableProjectModelBuilder {

  private final MuleProjectStructure projectStructure;
  private final ArtifactCoordinates deployableArtifactCoordinates;

  public MavenRefreshDeployableProjectModelBuilder(MuleProjectStructure projectStructure,
                                                   ArtifactCoordinates deployableArtifactCoordinates,
                                                   boolean exportAllResourcesAndPackagesIfEmptyLoaderDescriptor,
                                                   List<BundleDependency> deployableBundleDependencies,
                                                   Set<BundleDescriptor> sharedDeployableBundleDescriptors,
                                                   Map<BundleDescriptor, List<BundleDependency>> additionalPluginDependencies,
                                                   MavenConfiguration mavenConfiguration) {
    super(projectStructure.getProjectFolder().toFile(),
          mavenConfiguration != null ? mavenConfiguration : DEFAULT_MAVEN_CONFIGURATION.get(),
          exportAllResourcesAndPackagesIfEmptyLoaderDescriptor, false);

    this.projectStructure = projectStructure;
    this.deployableArtifactCoordinates = deployableArtifactCoordinates;

    this.deployableBundleDependencies = deployableBundleDependencies;
    this.sharedDeployableBundleDescriptors = sharedDeployableBundleDescriptors;
    this.additionalPluginDependencies = additionalPluginDependencies;
  }

  @Override
  public DeployableProjectModel build() {
    return doBuild(projectStructure,
                   new org.mule.tools.api.classloader.model.ArtifactCoordinates(deployableArtifactCoordinates.getGroupId(),
                                                                                deployableArtifactCoordinates.getArtifactId(),
                                                                                deployableArtifactCoordinates.getVersion(),
                                                                                "jar",
                                                                                deployableArtifactCoordinates.getClassifier()
                                                                                    .orElse(null)));
  }

}
