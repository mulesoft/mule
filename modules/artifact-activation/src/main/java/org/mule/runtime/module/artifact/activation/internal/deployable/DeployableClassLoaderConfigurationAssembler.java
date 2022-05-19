/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.deployable;

import org.mule.runtime.api.deployment.meta.MuleDeployableModel;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.internal.classloader.AbstractArtifactClassLoaderConfigurationAssembler;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Assembles the class loader configuration for a deployable artifact.
 *
 * @param <M> type of the model of the artifact owning the class loader configuration.
 */
public class DeployableClassLoaderConfigurationAssembler<M extends MuleDeployableModel>
    extends AbstractArtifactClassLoaderConfigurationAssembler {

  DeployableProjectModel<M> deployableProjectModel;

  public DeployableClassLoaderConfigurationAssembler(DeployableProjectModel<M> model) {
    super(new DeployableClassLoaderModelAssembler<>(model).createClassLoaderModel());
    deployableProjectModel = model;
  }

  @Override
  protected List<BundleDependency> getBundleDependencies() {
    return deployableProjectModel.getAppBundleDependencies();
  }

  @Override
  protected Set<String> getExportedPackages() {
    return new HashSet<>(deployableProjectModel.getExportedPackages());
  }

  @Override
  protected Set<String> getExportedResources() {
    return new HashSet<>(deployableProjectModel.getExportedResources());
  }

  @Override
  protected File getProjectFolder() {
    return deployableProjectModel.getProjectFolder();
  }


}
