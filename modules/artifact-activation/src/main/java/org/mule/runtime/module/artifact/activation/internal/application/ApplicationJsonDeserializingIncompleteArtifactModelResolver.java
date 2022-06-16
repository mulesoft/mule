/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.application;

import org.mule.runtime.api.deployment.meta.MuleApplicationModel;
import org.mule.runtime.api.deployment.persistence.AbstractMuleArtifactModelJsonSerializer;
import org.mule.runtime.module.artifact.activation.internal.deployable.AbstractDefaultValuesMuleDeployableModelGenerator;
import org.mule.runtime.module.artifact.activation.internal.deployable.AbstractJsonDeserializingIncompleteArtifactModelResolver;
import org.mule.runtime.module.artifact.activation.internal.descriptor.ConfigurationsResolver;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

import java.io.File;
import java.util.List;

/**
 * Loads a {@link MuleApplicationModel} ensuring it's completed with default values.
 */
public class ApplicationJsonDeserializingIncompleteArtifactModelResolver extends
    AbstractJsonDeserializingIncompleteArtifactModelResolver<MuleApplicationModel, MuleApplicationModel.MuleApplicationModelBuilder> {

  public ApplicationJsonDeserializingIncompleteArtifactModelResolver(AbstractMuleArtifactModelJsonSerializer<MuleApplicationModel> jsonDeserializer,
                                                                     String modelConfigsDirectory,
                                                                     BundleDescriptor modelBundleDescriptor,
                                                                     List<BundleDependency> modelDependencies,
                                                                     List<BundleDependency> modelMuleRuntimeDependencies,
                                                                     List<String> modelPackages, List<String> modelResources,
                                                                     ConfigurationsResolver configurationsResolver) {
    super(jsonDeserializer, modelConfigsDirectory, modelBundleDescriptor, modelDependencies, modelMuleRuntimeDependencies,
          modelPackages,
          modelResources, configurationsResolver);
  }

  @Override
  protected AbstractDefaultValuesMuleDeployableModelGenerator<MuleApplicationModel, MuleApplicationModel.MuleApplicationModelBuilder> getDefaultValuesMuleArtifactJsonGenerator(MuleApplicationModel originalMuleDeployableModel,
                                                                                                                                                                                File artifactLocation,
                                                                                                                                                                                String modelConfigsDirectory,
                                                                                                                                                                                BundleDescriptor modelBundleDescriptor,
                                                                                                                                                                                List<BundleDependency> modelDependencies,
                                                                                                                                                                                List<BundleDependency> modelMuleRuntimeDependencies,
                                                                                                                                                                                List<String> modelPackages,
                                                                                                                                                                                List<String> modelResources,
                                                                                                                                                                                ConfigurationsResolver configurationsResolver) {
    return new ApplicationDefaultValuesMuleDeployableModelGenerator(originalMuleDeployableModel, artifactLocation,
                                                                    modelConfigsDirectory, modelBundleDescriptor,
                                                                    modelDependencies,
                                                                    modelMuleRuntimeDependencies, modelPackages, modelResources,
                                                                    configurationsResolver,
                                                                    new MuleApplicationModel.MuleApplicationModelBuilder());
  }

}
