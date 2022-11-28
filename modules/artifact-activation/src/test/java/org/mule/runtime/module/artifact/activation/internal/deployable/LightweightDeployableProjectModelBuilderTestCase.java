/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.deployable;

import static org.mule.runtime.module.artifact.activation.internal.MavenTestUtils.installArtifact;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.CLASSLOADING_ISOLATION;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.ARTIFACT_DESCRIPTORS;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;

import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.internal.maven.LightweightDeployableProjectModelBuilder;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@Feature(CLASSLOADING_ISOLATION)
@Story(ARTIFACT_DESCRIPTORS)
public class LightweightDeployableProjectModelBuilderTestCase {

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public SystemProperty repositoryLocation =
      new SystemProperty("muleRuntimeConfig.maven.repositoryLocation", temporaryFolder.newFolder().getAbsolutePath());
  @Rule
  public SystemProperty localPluginDirectory =
      new SystemProperty("localPluginDirectory", temporaryFolder.newFolder().getAbsolutePath());

  public LightweightDeployableProjectModelBuilderTestCase() throws IOException {}

  @Test
  public void createDeployableProjectModelWithSystemScopePlugin() throws Exception {
    installArtifact(getResourceFolder("dependencies/plugin-with-transitive-dependency"),
                    new File(localPluginDirectory.getValue()));
    installArtifact(getResourceFolder("dependencies/library-1.0.0.pom"), new File(repositoryLocation.getValue()));

    DeployableProjectModel deployableProjectModel = getDeployableProjectModel("apps/lightweight/plugin-dependency-as-system");

    assertThat(deployableProjectModel.getDependencies(),
               contains(hasProperty("descriptor", hasProperty("artifactId", equalTo("plugin-with-transitive-dependency")))));
    assertThat(deployableProjectModel.getDependencies().get(0).getTransitiveDependenciesList(),
               contains(hasProperty("descriptor", hasProperty("artifactId", equalTo("library")))));
  }

  private DeployableProjectModel getDeployableProjectModel(String deployablePath) throws URISyntaxException {
    DeployableProjectModel model = new LightweightDeployableProjectModelBuilder(getResourceFolder(deployablePath), false).build();

    model.validate();

    return model;
  }

  protected File getResourceFolder(String appPath) throws URISyntaxException {
    return new File(getClass().getClassLoader().getResource(appPath).toURI());
  }

}
