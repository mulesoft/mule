/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.deployable;

import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor.MULE_DOMAIN_CLASSIFIER;

import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.internal.classloader.AbstractArtifactClassLoaderConfigurationAssembler;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration.ClassLoaderConfigurationBuilder;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Assembles the class loader configuration for a deployable artifact.
 */
public class DeployableClassLoaderConfigurationAssembler extends AbstractArtifactClassLoaderConfigurationAssembler {

  private final DeployableProjectModel deployableProjectModel;

  public DeployableClassLoaderConfigurationAssembler(DeployableProjectModel deployableProjectModel,
                                                     MuleArtifactLoaderDescriptor muleArtifactLoaderDescriptor) {
    super(deployableProjectModel.getDescriptor(),
          muleArtifactLoaderDescriptor);
    this.deployableProjectModel = deployableProjectModel;
  }

  @Override
  protected List<BundleDependency> getBundleDependencies() {
    return deployableProjectModel.getDependencies();
  }

  @Override
  protected File getProjectFolder() {
    return deployableProjectModel.getProjectFolder();
  }

  @Override
  protected ClassLoaderConfiguration.ClassLoaderConfigurationBuilder getClassLoaderConfigurationBuilder() {
    return new DeployableClassLoaderConfigurationBuilder(deployableProjectModel, getProjectFolder());
  }

  @Override
  protected void populateLocalPackages(ClassLoaderConfigurationBuilder classLoaderConfigurationBuilder) {
    Set<String> packagesSetBuilder = new HashSet<>();
    if (deployableProjectModel.getPackages() != null) {
      packagesSetBuilder.addAll(deployableProjectModel.getPackages());
    }

    Set<String> resourcesSetBuilder = new HashSet<>();
    if (deployableProjectModel.getResources() != null) {
      resourcesSetBuilder.addAll(deployableProjectModel.getResources());
    }

    deployableProjectModel.getDependencies().forEach(dependency -> {
      if (!dependency.getDescriptor().getClassifier().map(MULE_PLUGIN_CLASSIFIER::equals).orElse(false)
          && !dependency.getDescriptor().getClassifier().map(MULE_DOMAIN_CLASSIFIER::equals).orElse(false)
          && !validateMuleRuntimeSharedLibrary(dependency.getDescriptor().getGroupId(),
                                               dependency.getDescriptor().getArtifactId(),
                                               deployableProjectModel.getDescriptor().getArtifactId())
          && dependency.getBundleUri() != null) {
        if (dependency.getPackages() != null) {
          packagesSetBuilder.addAll(dependency.getPackages());
        }
        if (dependency.getResources() != null) {
          resourcesSetBuilder.addAll(dependency.getResources());
        }
      }
    });

    classLoaderConfigurationBuilder.withLocalPackages(packagesSetBuilder);
    classLoaderConfigurationBuilder.withLocalResources(resourcesSetBuilder);
  }
}
