/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.domain;

import org.mule.runtime.api.deployment.meta.MuleDomainModel;
import org.mule.runtime.api.deployment.persistence.AbstractMuleArtifactModelJsonSerializer;
import org.mule.runtime.module.artifact.activation.internal.deployable.AbstractDefaultValuesMuleDeployableModelGenerator;
import org.mule.runtime.module.artifact.activation.internal.deployable.AbstractJsonDeserializingIncompleteArtifactModelResolver;
import org.mule.runtime.module.artifact.activation.internal.descriptor.ConfigurationsResolver;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

import java.io.File;
import java.util.List;

/**
 * Loads a {@link MuleDomainModel} ensuring it's completed with default values.
 */
public class DomainJsonDeserializingIncompleteArtifactModelResolver
    extends AbstractJsonDeserializingIncompleteArtifactModelResolver<MuleDomainModel, MuleDomainModel.MuleDomainModelBuilder> {

  public DomainJsonDeserializingIncompleteArtifactModelResolver(AbstractMuleArtifactModelJsonSerializer<MuleDomainModel> jsonDeserializer,
                                                                String modelConfigsDirectory,
                                                                BundleDescriptor modelBundleDescriptor,
                                                                List<BundleDependency> modelDependencies,
                                                                List<BundleDependency> modelMuleRuntimeDependencies,
                                                                List<String> modelPackages,
                                                                List<String> modelResources,
                                                                ConfigurationsResolver configurationsResolver) {
    super(jsonDeserializer, modelConfigsDirectory, modelBundleDescriptor, modelDependencies, modelMuleRuntimeDependencies,
          modelPackages,
          modelResources, configurationsResolver);
  }

  @Override
  protected AbstractDefaultValuesMuleDeployableModelGenerator<MuleDomainModel, MuleDomainModel.MuleDomainModelBuilder> getDefaultValuesMuleArtifactJsonGenerator(MuleDomainModel originalMuleDeployableModel,
                                                                                                                                                                 File artifactLocation,
                                                                                                                                                                 String modelConfigsDirectory,
                                                                                                                                                                 BundleDescriptor modelBundleDescriptor,
                                                                                                                                                                 List<BundleDependency> modelDependencies,
                                                                                                                                                                 List<BundleDependency> modelMuleRuntimeDependencies,
                                                                                                                                                                 List<String> modelPackages,
                                                                                                                                                                 List<String> modelResources,
                                                                                                                                                                 ConfigurationsResolver configurationsResolver) {
    return new DomainDefaultValuesMuleDeployableModelGenerator(originalMuleDeployableModel, artifactLocation,
                                                               modelConfigsDirectory, modelBundleDescriptor, modelDependencies,
                                                               modelMuleRuntimeDependencies, modelPackages, modelResources,
                                                               configurationsResolver,
                                                               new MuleDomainModel.MuleDomainModelBuilder());
  }

}
