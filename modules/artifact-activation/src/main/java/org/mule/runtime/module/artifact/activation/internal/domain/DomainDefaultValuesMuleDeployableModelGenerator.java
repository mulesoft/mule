/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.domain;

import org.mule.runtime.api.deployment.meta.MuleDomainModel;
import org.mule.runtime.module.artifact.activation.internal.deployable.AbstractDefaultValuesMuleDeployableModelGenerator;
import org.mule.runtime.module.artifact.activation.internal.descriptor.ConfigurationsResolver;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * Generates default values for any non-defined fields in a {@link MuleDomainModel}.
 */
public class DomainDefaultValuesMuleDeployableModelGenerator
    extends AbstractDefaultValuesMuleDeployableModelGenerator<MuleDomainModel, MuleDomainModel.MuleDomainModelBuilder> {

  public DomainDefaultValuesMuleDeployableModelGenerator(MuleDomainModel originalMuleDeployableModel,
                                                         File artifactLocation,
                                                         String configsDirectory,
                                                         BundleDescriptor modelBundleDescriptor,
                                                         List<BundleDependency> modelDependencies,
                                                         List<BundleDependency> modelMuleRuntimeDependencies,
                                                         List<String> modelPackages,
                                                         List<String> modelResources,
                                                         ConfigurationsResolver configurationsResolver,
                                                         MuleDomainModel.MuleDomainModelBuilder builder) {
    super(originalMuleDeployableModel, artifactLocation, configsDirectory, modelBundleDescriptor, modelDependencies,
          modelMuleRuntimeDependencies,
          modelPackages, modelResources, configurationsResolver, builder);
  }

  @Override
  protected void doSetBuilderWithConfigFile(String logConfigFile) {
    getBuilder().setLogConfigFile(logConfigFile);
  }

  @Override
  protected void doSetBuilderWithDefaultSecureProperties(List<String> secureProperties) {
    getBuilder().setSecureProperties(secureProperties);
  }

  @Override
  protected void doSetBuilderWithDefaultRedeploymentEnabled(boolean redeploymentEnabled) {
    getBuilder().setRedeploymentEnabled(redeploymentEnabled);
  }

  @Override
  protected void doSetBuilderWithDefaultConfigsValue(Set<String> defaultConfigs) {
    getBuilder().setConfigs(defaultConfigs);
  }

}
