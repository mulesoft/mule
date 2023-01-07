/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.deployable;

import static org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor.MULE_APPLICATION_CLASSIFIER;
import static org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor.MULE_DOMAIN_CLASSIFIER;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.CLASSLOADING_ISOLATION;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.ARTIFACT_DESCRIPTORS;

import static java.lang.String.format;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.rules.ExpectedException.none;

import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(CLASSLOADING_ISOLATION)
@Story(ARTIFACT_DESCRIPTORS)
public class MuleDeployableProjectModelBuilderTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void createBasicDeployableProjectModel() throws Exception {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel("apps/heavyweight/basic");

    testBasicDeployableProjectModel(deployableProjectModel);
  }

  @Test
  public void patchedApplicationLoadsUpdatedConnector() throws Exception {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel("apps/heavyweight/patched-plugin-app");

    testPatchedDependency(deployableProjectModel.getDependencies(), "mule-objectstore-connector", "1.1.0");
  }

  @Test
  public void patchedApplicationLoadsUpdatedJar() throws Exception {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel("apps/heavyweight/patched-jar-app");

    testPatchedDependency(deployableProjectModel.getDependencies(), "commons-cli", "1.4");
  }

  @Test
  public void patchedApplicationLoadsUpdatedJarAndPlugin() throws Exception {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel("apps/heavyweight/patched-jar-and-plugin-app");

    testPatchedDependency(deployableProjectModel.getDependencies(), "mule-objectstore-connector", "1.1.0");
    testPatchedDependency(deployableProjectModel.getDependencies(), "commons-cli", "1.4");
  }

  private void testPatchedDependency(List<BundleDependency> dependencies, String dependencyName, String expectedVersion) {
    BundleDependency dependency = dependencies
        .stream()
        .filter(d -> d.getDescriptor().getArtifactId().equals(dependencyName))
        .findFirst()
        .get();

    assertThat(dependency.getDescriptor().getVersion(), is(expectedVersion));
  }

  @Test
  public void createDeployableProjectModelWithSharedLibrary() throws URISyntaxException {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel("apps/heavyweight/shared-lib");

    // checks there are no packages in the project
    assertThat(deployableProjectModel.getPackages(), hasSize(0));

    assertThat(deployableProjectModel.getSharedLibraries(), contains(hasProperty("artifactId", equalTo("derby"))));
  }

  @Test
  public void createDeployableProjectModelWithTransitiveSharedLibrary() throws URISyntaxException {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel("apps/heavyweight/shared-lib-transitive");

    assertThat(deployableProjectModel.getSharedLibraries(), hasSize(6));
    assertThat(deployableProjectModel.getSharedLibraries(), hasItem(hasProperty("artifactId", equalTo("spring-context"))));
  }

  @Test
  public void createDeployableProjectModelWithAdditionalPluginDependency() throws URISyntaxException {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel("apps/heavyweight/additional-plugin-dependency");

    assertThat(deployableProjectModel.getDependencies(), hasSize(3));
    assertThat(deployableProjectModel.getDependencies(),
               hasItem(hasProperty("descriptor", hasProperty("artifactId", equalTo("mule-db-connector")))));

    assertThat(deployableProjectModel.getAdditionalPluginDependencies(), aMapWithSize(1));
    assertThat(deployableProjectModel.getAdditionalPluginDependencies(),
               hasEntry(hasProperty("artifactId", equalTo("mule-db-connector")),
                        contains(hasProperty("descriptor", hasProperty("artifactId", equalTo("derby"))))));
  }

  @Test
  public void createDeployableProjectModelWithAdditionalPluginDependencyAndDependency() throws URISyntaxException {
    DeployableProjectModel deployableProjectModel =
        getDeployableProjectModel("apps/heavyweight/additional-plugin-dependency-and-dep");

    assertThat(deployableProjectModel.getDependencies(), hasSize(4));
    assertThat(deployableProjectModel.getDependencies(),
               hasItems(hasProperty("descriptor", hasProperty("artifactId", equalTo("derby"))),
                        hasProperty("descriptor", hasProperty("artifactId", equalTo("mule-db-connector")))));

    assertThat(deployableProjectModel.getAdditionalPluginDependencies(), aMapWithSize(1));
    assertThat(deployableProjectModel.getAdditionalPluginDependencies(),
               hasEntry(hasProperty("artifactId", equalTo("mule-db-connector")),
                        contains(hasProperty("descriptor", hasProperty("artifactId", equalTo("derby"))))));
  }

  @Test
  public void createDeployableProjectModelWithTransitiveAdditionalPluginDependency() throws URISyntaxException {
    DeployableProjectModel deployableProjectModel =
        getDeployableProjectModel("apps/heavyweight/additional-plugin-dependency-transitive");

    assertThat(deployableProjectModel.getDependencies(), hasSize(1));
    assertThat(deployableProjectModel.getDependencies(),
               hasItem(hasProperty("descriptor", hasProperty("artifactId", equalTo("mule-spring-module")))));

    assertThat(deployableProjectModel.getAdditionalPluginDependencies(), aMapWithSize(1));
    assertThat(deployableProjectModel.getAdditionalPluginDependencies(),
               hasEntry(hasProperty("artifactId", equalTo("mule-spring-module")), hasSize(6)));
    assertThat(deployableProjectModel.getAdditionalPluginDependencies(),
               hasEntry(hasProperty("artifactId", equalTo("mule-spring-module")),
                        hasItem(hasProperty("descriptor", hasProperty("artifactId", equalTo("spring-context"))))));
  }

  @Test
  @Issue("W-11799074")
  @Description("Tests that when a deployable is packaged using the lightweight option that generates the " +
      "classloader-model.json for its dependencies, and thus the deployable is considered for deployment as a heavy " +
      "package even though its dependencies aren't really packaged within it, the model is correctly built.")
  public void createDeployableProjectModelFromLightweightLocalRepository() throws URISyntaxException {
    DeployableProjectModel deployableProjectModel =
        getDeployableProjectModel("apps/heavyweight/basic-lightweight-local-repository");

    testBasicDeployableProjectModel(deployableProjectModel);
  }

  @Test
  @Issue("W-12276189")
  public void projectWithInvalidClassLoaderModel() throws URISyntaxException {
    expectedException.expect(IllegalArgumentException.class);
    expectedException
        .expectMessage(format("Artifact coordinates from project's class loader model are missing the 'classifier' attribute. Valid values are '%s' and '%s'.",
                              MULE_APPLICATION_CLASSIFIER, MULE_DOMAIN_CLASSIFIER));

    getDeployableProjectModel("apps/heavyweight/invalid-classloader-model");
  }

  private void testBasicDeployableProjectModel(DeployableProjectModel deployableProjectModel) {
    assertThat(deployableProjectModel.getPackages(), contains("org.test"));
    assertThat(deployableProjectModel.getResources(),
               containsInAnyOrder("test-script.dwl", "app.xml"));

    assertThat(deployableProjectModel.getDependencies(), hasSize(3));

    Map<String, Integer> transitiveDependenciesNum =
        of("mule-http-connector", 13, "mule-sockets-connector", 7, "mule-db-connector", 10);

    deployableProjectModel.getDependencies()
        .forEach(dependency -> assertThat(dependency.getTransitiveDependenciesList(),
                                          hasSize(transitiveDependenciesNum.get(dependency.getDescriptor().getArtifactId()))));

    assertThat(deployableProjectModel.getSharedLibraries(), hasSize(0));
    assertThat(deployableProjectModel.getAdditionalPluginDependencies(), aMapWithSize(0));

    // checks the Mule deployable model can be correctly deserialized
    deployableProjectModel.getDeployableModel().validateModel("basic");
  }

  private DeployableProjectModel getDeployableProjectModel(String deployablePath) throws URISyntaxException {
    DeployableProjectModel model = new MuleDeployableProjectModelBuilder(getDeployableFolder(deployablePath)).build();

    model.validate();

    return model;
  }

  protected File getDeployableFolder(String appPath) throws URISyntaxException {
    return new File(getClass().getClassLoader().getResource(appPath).toURI());
  }

}
