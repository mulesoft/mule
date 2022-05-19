/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.classloader.model;

import org.mule.tools.api.classloader.model.Artifact;
import org.mule.tools.api.classloader.model.ArtifactCoordinates;
import org.mule.tools.api.classloader.model.ClassLoaderModel;

import java.util.List;

/**
 * Assembles the class loader model for an artifact given all its pieces.
 */
public class ClassLoaderModelAssembler {

  protected static final String CLASS_LOADER_MODEL_VERSION = "1.2.0";

  protected final ArtifactCoordinates artifactCoordinates;
  protected final List<Artifact> projectDependencies;
  protected final List<String> exportedPackages;
  protected final List<String> exportedResources;

  public ClassLoaderModelAssembler(ArtifactCoordinates artifactCoordinates, List<Artifact> projectDependencies,
                                   List<String> exportedPackages, List<String> exportedResources) {
    this.artifactCoordinates = artifactCoordinates;
    this.projectDependencies = projectDependencies;
    this.exportedPackages = exportedPackages;
    this.exportedResources = exportedResources;
  }

  public ClassLoaderModel createClassLoaderModel() {
    ClassLoaderModel classLoaderModel = new ClassLoaderModel(CLASS_LOADER_MODEL_VERSION, artifactCoordinates);

    assembleClassLoaderModel(classLoaderModel);

    return classLoaderModel;
  }

  protected final void assembleClassLoaderModel(ClassLoaderModel classLoaderModel) {
    classLoaderModel.setDependencies(projectDependencies);
    classLoaderModel.setPackages(exportedPackages.toArray(new String[0]));
    classLoaderModel.setResources(exportedResources.toArray(new String[0]));
  }

}
