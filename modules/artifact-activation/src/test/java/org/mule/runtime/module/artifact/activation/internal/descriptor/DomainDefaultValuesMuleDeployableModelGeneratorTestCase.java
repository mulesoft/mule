/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.descriptor;

import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MuleDomainModel;
import org.mule.runtime.api.deployment.meta.Product;
import org.mule.runtime.api.deployment.persistence.AbstractMuleArtifactModelJsonSerializer;
import org.mule.runtime.api.deployment.persistence.MuleDomainModelJsonSerializer;
import org.mule.runtime.module.artifact.activation.internal.domain.DomainDefaultValuesMuleDeployableModelGenerator;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

import java.io.File;
import java.util.List;
import java.util.Set;

public class DomainDefaultValuesMuleDeployableModelGeneratorTestCase
    extends AbstractDefaultValuesMuleDeployableModelGeneratorTestCase<MuleDomainModel> {

  private final BundleDescriptor domainDescriptor = new BundleDescriptor.Builder()
      .setGroupId("org.mule.sample")
      .setArtifactId("test-domain")
      .setVersion("0.0.1")
      .setClassifier("mule-domain")
      .build();

  @Override
  protected MuleDomainModel completeModel(MuleDomainModel originalModel, File artifactLocation, String modelConfigsDirectory,
                                          List<BundleDependency> modelDependencies,
                                          List<BundleDependency> modelMuleRuntimeDependencies, List<String> modelPackages,
                                          List<String> modelResources) {
    return new DomainDefaultValuesMuleDeployableModelGenerator(originalModel, artifactLocation, modelConfigsDirectory,
                                                               domainDescriptor, modelDependencies, modelMuleRuntimeDependencies,
                                                               modelPackages, modelResources, getBuilder()).generate();
  }

  @Override
  protected MuleDomainModel getModel(String minMuleVersion, String name, Product requiredProduct,
                                     MuleArtifactLoaderDescriptor classLoaderModelLoaderDescriptor,
                                     MuleArtifactLoaderDescriptor bundleDescriptorLoaderDescriptor, List<String> secureProperties,
                                     boolean redeploymentEnabled, Set<String> configs, String logConfigFile) {
    MuleDomainModel.MuleDomainModelBuilder builder = getBuilder()
        .setMinMuleVersion("4.4.0")
        .setName(name)
        .setRequiredProduct(requiredProduct)
        .withClassLoaderModelDescriptorLoader(classLoaderModelLoaderDescriptor)
        .withBundleDescriptorLoader(bundleDescriptorLoaderDescriptor);
    builder.setSecureProperties(secureProperties);
    builder.setRedeploymentEnabled(redeploymentEnabled);
    builder.setConfigs(configs);
    builder.setLogConfigFile(logConfigFile);

    return builder.build();
  }

  @Override
  protected AbstractMuleArtifactModelJsonSerializer<MuleDomainModel> getModelDeserializer() {
    return new MuleDomainModelJsonSerializer();
  }

  @Override
  protected BundleDescriptor getDescriptor() {
    return domainDescriptor;
  }

  private MuleDomainModel.MuleDomainModelBuilder getBuilder() {
    return new MuleDomainModel.MuleDomainModelBuilder();
  }

}
