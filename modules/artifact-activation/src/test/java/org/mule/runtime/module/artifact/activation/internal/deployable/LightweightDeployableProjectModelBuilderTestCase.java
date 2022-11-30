/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.deployable;

import static org.mule.runtime.core.api.util.FileUtils.cleanDirectory;
import static org.mule.runtime.module.artifact.activation.internal.MavenTestUtils.installArtifact;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.CLASSLOADING_ISOLATION;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.ARTIFACT_DESCRIPTORS;

import static java.lang.Math.random;
import static java.nio.file.Files.createTempDirectory;

import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;

import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.internal.maven.LightweightDeployableProjectModelBuilder;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;

@Feature(CLASSLOADING_ISOLATION)
@Story(ARTIFACT_DESCRIPTORS)
@Issue("W-12069164")
public class LightweightDeployableProjectModelBuilderTestCase {

  @ClassRule
  public static SystemProperty repositoryLocation;

  @ClassRule
  public static SystemProperty localPluginDirectory;

  static {
    try {
      repositoryLocation = new SystemProperty("muleRuntimeConfig.maven.repositoryLocation",
                                              createTempDirectory("localRepository" + random()).toFile().getAbsolutePath());
      localPluginDirectory =
          new SystemProperty("localPluginDirectory",
                             createTempDirectory("localPluginDirectory" + random()).toFile().getAbsolutePath());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @After
  public void after() throws IOException {
    cleanDirectory(new File(repositoryLocation.getValue()));
    cleanDirectory(new File(localPluginDirectory.getValue()));
  }

  @AfterClass
  public static void afterClass() {
    deleteQuietly(new File(repositoryLocation.getValue()));
    deleteQuietly(new File(localPluginDirectory.getValue()));
  }

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

  @Test
  public void createDeployableProjectModelWithSystemScopePluginWithTransitivePluginDependency() throws Exception {
    installArtifact(getResourceFolder("dependencies/plugin-with-transitive-plugin-dependency"),
                    new File(localPluginDirectory.getValue()));
    installArtifact(getResourceFolder("dependencies/plugin-with-transitive-dependency"),
                    new File(repositoryLocation.getValue()));
    installArtifact(getResourceFolder("dependencies/library-1.0.0.pom"), new File(repositoryLocation.getValue()));

    DeployableProjectModel deployableProjectModel =
        getDeployableProjectModel("apps/lightweight/plugin-dependency-with-transitive-plugin-dependency-as-system");

    assertThat(deployableProjectModel.getDependencies(),
               containsInAnyOrder(hasProperty("descriptor",
                                              hasProperty("artifactId", equalTo("plugin-with-transitive-plugin-dependency"))),
                                  hasProperty("descriptor",
                                              hasProperty("artifactId", equalTo("plugin-with-transitive-dependency")))));

    BundleDependency pluginWithTransitivePluginDependency = deployableProjectModel.getDependencies().stream()
        .filter(d -> d.getDescriptor().getArtifactId().equals("plugin-with-transitive-plugin-dependency")).findFirst().get();
    assertThat(pluginWithTransitivePluginDependency.getTransitiveDependenciesList(),
               contains(hasProperty("descriptor", hasProperty("artifactId", equalTo("plugin-with-transitive-dependency")))));

    BundleDependency pluginWithTransitiveDependency = deployableProjectModel.getDependencies().stream()
        .filter(d -> d.getDescriptor().getArtifactId().equals("plugin-with-transitive-dependency")).findFirst().get();
    assertThat(pluginWithTransitiveDependency.getTransitiveDependenciesList(),
               contains(hasProperty("descriptor", hasProperty("artifactId", equalTo("library")))));
  }

  @Test
  @Description("Tests that transitive dependencies from a plugin with system scope that are plugins, are set as dependencies " +
      "in the DeployableProjectModel, and also if any of them is already considered as such, they're not duplicated.")
  public void createDeployableProjectModelWithSystemScopePluginWithNestedTransitivePluginDependenciesAlreadyPresentInDeployablePom()
      throws Exception {
    installArtifact(getResourceFolder("dependencies/plugin-with-nested-transitive-plugin-dependencies"),
                    new File(localPluginDirectory.getValue()));
    installArtifact(getResourceFolder("dependencies/plugin-with-transitive-plugin-dependency"),
                    new File(repositoryLocation.getValue()));
    installArtifact(getResourceFolder("dependencies/plugin-with-transitive-dependency"),
                    new File(repositoryLocation.getValue()));
    installArtifact(getResourceFolder("dependencies/library-1.0.0.pom"), new File(repositoryLocation.getValue()));

    DeployableProjectModel deployableProjectModel =
        getDeployableProjectModel("apps/lightweight/plugin-dependency-with-nested-transitive-plugin-dependencies-as-system");

    assertThat(deployableProjectModel.getDependencies(),
               containsInAnyOrder(hasProperty("descriptor",
                                              hasProperty("artifactId",
                                                          equalTo("plugin-with-nested-transitive-plugin-dependencies"))),
                                  hasProperty("descriptor",
                                              hasProperty("artifactId", equalTo("plugin-with-transitive-plugin-dependency"))),
                                  hasProperty("descriptor",
                                              hasProperty("artifactId", equalTo("plugin-with-transitive-dependency")))));

    BundleDependency pluginWithNestedTransitivePluginDependencies = deployableProjectModel.getDependencies().stream()
        .filter(d -> d.getDescriptor().getArtifactId().equals("plugin-with-nested-transitive-plugin-dependencies")).findFirst()
        .get();
    assertThat(pluginWithNestedTransitivePluginDependencies.getTransitiveDependenciesList(),
               contains(hasProperty("descriptor",
                                    hasProperty("artifactId", equalTo("plugin-with-transitive-plugin-dependency")))));

    BundleDependency pluginWithTransitivePluginDependency = deployableProjectModel.getDependencies().stream()
        .filter(d -> d.getDescriptor().getArtifactId().equals("plugin-with-transitive-plugin-dependency")).findFirst().get();
    assertThat(pluginWithTransitivePluginDependency.getTransitiveDependenciesList(),
               contains(hasProperty("descriptor", hasProperty("artifactId", equalTo("plugin-with-transitive-dependency")))));

    BundleDependency pluginWithTransitiveDependency = deployableProjectModel.getDependencies().stream()
        .filter(d -> d.getDescriptor().getArtifactId().equals("plugin-with-transitive-dependency")).findFirst().get();
    assertThat(pluginWithTransitiveDependency.getTransitiveDependenciesList(),
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
