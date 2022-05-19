/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.deployable;

import org.mule.runtime.api.deployment.meta.MuleDeployableModel;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.internal.classloader.model.ClassLoaderModelAssembler;
import org.mule.tools.api.classloader.model.AppClassLoaderModel;
import org.mule.tools.api.classloader.model.ClassLoaderModel;
import org.mule.tools.api.classloader.model.Plugin;

import java.util.List;

/**
 * Assembles the class loader model for a deployable artifact.
 *
 * @param <M> type of the model of the artifact owning the class loader model.
 */
public class DeployableClassLoaderModelAssembler<M extends MuleDeployableModel> extends ClassLoaderModelAssembler {

  private final List<Plugin> additionalPluginDependencies;

  public DeployableClassLoaderModelAssembler(DeployableProjectModel<M> model) {
    super(model.getArtifactCoordinates(), model.getProjectDependencies(), model.getExportedPackages(),
          model.getExportedResources());
    additionalPluginDependencies = model.getAdditionalPluginDependencies();
  }

  @Override
  public ClassLoaderModel createClassLoaderModel() {
    AppClassLoaderModel deployableModel = new AppClassLoaderModel(CLASS_LOADER_MODEL_VERSION, artifactCoordinates);
    assembleClassLoaderModel(deployableModel);
    deployableModel.setAdditionalPluginDependencies(additionalPluginDependencies);

    return deployableModel;
  }
}
