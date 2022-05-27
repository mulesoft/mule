/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.classloader.model;

import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.tools.api.classloader.model.Artifact;
import org.mule.tools.api.classloader.model.ArtifactCoordinates;
import org.mule.tools.api.classloader.model.ClassLoaderModel;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

/**
 * Assembles the class loader model for an artifact given all its pieces.
 */
public class ClassLoaderModelAssembler {

  protected static final String CLASS_LOADER_MODEL_VERSION = "1.2.0";

  protected final ArtifactCoordinates artifactCoordinates;
  protected final List<Artifact> projectDependencies;
  protected final List<String> availablePackages;
  protected final List<String> availableResources;
  private final MuleArtifactLoaderDescriptor muleArtifactLoaderDescriptor;

  public ClassLoaderModelAssembler(ArtifactCoordinates artifactCoordinates, List<Artifact> projectDependencies,
                                   List<String> availablePackages, List<String> availableResources,
                                   MuleArtifactLoaderDescriptor muleArtifactLoaderDescriptor) {
    this.artifactCoordinates = artifactCoordinates;
    this.projectDependencies = projectDependencies;
    this.availablePackages = availablePackages;
    this.availableResources = availableResources;
    this.muleArtifactLoaderDescriptor = muleArtifactLoaderDescriptor;
  }

  public ClassLoaderModelAssembler(ArtifactCoordinates artifactCoordinates, List<Artifact> dependencies,
                                   MuleArtifactLoaderDescriptor muleArtifactLoaderDescriptor) {
    this(artifactCoordinates, dependencies, emptyList(), emptyList(), muleArtifactLoaderDescriptor);
  }

  public ClassLoaderModel createClassLoaderModel() {
    ClassLoaderModel classLoaderModel = new ClassLoaderModel(CLASS_LOADER_MODEL_VERSION, artifactCoordinates);

    assembleClassLoaderModel(classLoaderModel);

    return classLoaderModel;
  }

  protected final void assembleClassLoaderModel(ClassLoaderModel classLoaderModel) {
    classLoaderModel.setDependencies(projectDependencies);
    classLoaderModel.setPackages(getExportedAttribute("exportedPackages", availablePackages));
    classLoaderModel.setResources(getExportedAttribute("exportedResources", availableResources));
  }

  private String[] getExportedAttribute(String exportedAttributeName, List<String> availableAttribute) {
    List<String> exportedAttribute = availableAttribute;
    if (muleArtifactLoaderDescriptor != null) {
      Map<String, Object> originalAttributes = muleArtifactLoaderDescriptor.getAttributes();
      if (originalAttributes != null && originalAttributes.get(exportedAttributeName) != null) {
        exportedAttribute = (List<String>) originalAttributes.get(exportedAttributeName);
      }
    }

    return exportedAttribute.toArray(new String[0]);
  }

}
