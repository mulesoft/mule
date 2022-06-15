/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.deployable;

import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.internal.classloader.AbstractArtifactClassLoaderConfigurationAssembler;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;

import java.io.File;
import java.util.List;

/**
 * Assembles the class loader configuration for a deployable artifact.
 */
public class DeployableClassLoaderConfigurationAssembler extends AbstractArtifactClassLoaderConfigurationAssembler {

  DeployableProjectModel deployableProjectModel;

  public DeployableClassLoaderConfigurationAssembler(DeployableProjectModel deployableProjectModel,
                                                     MuleArtifactLoaderDescriptor muleArtifactLoaderDescriptor) {
    super(new DeployableClassLoaderModelAssembler(deployableProjectModel, muleArtifactLoaderDescriptor)
        .createClassLoaderModel());
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
  protected ClassLoaderModel.ClassLoaderModelBuilder getClassLoaderConfigurationBuilder() {
    return new DeployableClassLoaderConfigurationBuilder(packagerClassLoaderModel, getProjectFolder());
  }

  @Override
  protected List<BundleDependency> getProcessedBundleDependencies() {
    // TODO W-11202346 - analyze patched dependencies in classloader-model-patch.json
    return super.getProcessedBundleDependencies();
  }

}
