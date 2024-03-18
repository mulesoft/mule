/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.deployable;

import static java.util.stream.Collectors.toList;

import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.internal.classloader.model.ClassLoaderModelAssembler;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.tools.api.classloader.model.AppClassLoaderModel;
import org.mule.tools.api.classloader.model.ArtifactCoordinates;
import org.mule.tools.api.classloader.model.ClassLoaderModel;
import org.mule.tools.api.classloader.model.Plugin;

import java.util.List;
import java.util.Map;

/**
 * Assembles the class loader model for a deployable artifact.
 */
public class DeployableClassLoaderModelAssembler extends ClassLoaderModelAssembler {

  private final Map<BundleDescriptor, List<BundleDependency>> additionalPluginDependencies;

  public DeployableClassLoaderModelAssembler(DeployableProjectModel model) {
    super(new ArtifactCoordinates(model.getDescriptor().getGroupId(),
                                  model.getDescriptor().getArtifactId(),
                                  model.getDescriptor().getVersion(),
                                  model.getDescriptor().getType(),
                                  model.getDescriptor().getClassifier().orElse(null)),
          model.getDependencies(),
          model.getSharedLibraries(),
          model.getPackages(),
          model.getResources());
    additionalPluginDependencies = model.getAdditionalPluginDependencies();
  }

  @Override
  public ClassLoaderModel createClassLoaderModel() {
    AppClassLoaderModel deployableModel = new AppClassLoaderModel(CLASS_LOADER_MODEL_VERSION, getArtifactCoordinates());
    assembleClassLoaderModel(deployableModel);

    deployableModel.setAdditionalPluginDependencies(additionalPluginDependencies.entrySet().stream()
        .map(pluginEntry -> {
          Plugin plugin = new Plugin();
          plugin.setArtifactId(pluginEntry.getKey().getArtifactId());
          plugin.setGroupId(pluginEntry.getKey().getGroupId());
          plugin.setAdditionalDependencies(toArtifacts(pluginEntry.getValue()));
          return plugin;
        })
        .collect(toList()));

    return deployableModel;
  }
}
