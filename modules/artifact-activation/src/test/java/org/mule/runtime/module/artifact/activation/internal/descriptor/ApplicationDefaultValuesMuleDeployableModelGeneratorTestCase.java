/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.descriptor;

import static java.util.Collections.emptyList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.mule.runtime.api.deployment.meta.MuleApplicationModel;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.Product;
import org.mule.runtime.api.deployment.persistence.AbstractMuleArtifactModelJsonSerializer;
import org.mule.runtime.api.deployment.persistence.MuleApplicationModelJsonSerializer;
import org.mule.runtime.module.artifact.activation.internal.application.ApplicationDefaultValuesMuleDeployableModelGenerator;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class ApplicationDefaultValuesMuleDeployableModelGeneratorTestCase
    extends AbstractDefaultValuesMuleDeployableModelGeneratorTestCase<MuleApplicationModel> {

  private final BundleDescriptor appDescriptor = new BundleDescriptor.Builder()
      .setGroupId("org.mule.sample")
      .setArtifactId("test-app")
      .setVersion("0.0.1")
      .setClassifier("mule-application")
      .build();

  @Test
  public void domainFieldIsPreserved() {
    MuleApplicationModel originalModel =
        getModel("{\"minMuleVersion\": \"4.4.0\", \"domain\":\"some-domain\"}");
    MuleApplicationModel model =
        completeModel(originalModel, temporaryFolder.getRoot(), DEFAULT_CONFIGS_DIRECTORY,
                      emptyList(), emptyList(), emptyList(),
                      emptyList());

    assertThat(model.getDomain(), is(originalModel.getDomain()));
  }

  @Override
  protected MuleApplicationModel completeModel(MuleApplicationModel originalModel, File artifactLocation,
                                               String modelConfigsDirectory, List<BundleDependency> modelDependencies,
                                               List<BundleDependency> modelMuleRuntimeDependencies, List<String> modelPackages,
                                               List<String> modelResources) {
    return new ApplicationDefaultValuesMuleDeployableModelGenerator(originalModel, artifactLocation, modelConfigsDirectory,
                                                                    appDescriptor, modelDependencies,
                                                                    modelMuleRuntimeDependencies, modelPackages, modelResources,
                                                                    new XmlConfigurationsResolver(),
                                                                    getBuilder()).generate();
  }

  @Override
  protected MuleApplicationModel getModel(String minMuleVersion, String name, Product requiredProduct,
                                          MuleArtifactLoaderDescriptor classLoaderModelLoaderDescriptor,
                                          MuleArtifactLoaderDescriptor bundleDescriptorLoaderDescriptor,
                                          List<String> secureProperties, boolean redeploymentEnabled, Set<String> configs,
                                          String logConfigFile) {
    MuleApplicationModel.MuleApplicationModelBuilder builder = getBuilder()
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
  protected AbstractMuleArtifactModelJsonSerializer<MuleApplicationModel> getModelDeserializer() {
    return new MuleApplicationModelJsonSerializer();
  }

  @Override
  protected BundleDescriptor getDescriptor() {
    return appDescriptor;
  }

  private MuleApplicationModel.MuleApplicationModelBuilder getBuilder() {
    return new MuleApplicationModel.MuleApplicationModelBuilder();
  }

}
