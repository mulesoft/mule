/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.deployable;

import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.CLASSLOADING_ISOLATION;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.ARTIFACT_DESCRIPTORS;

import static java.util.stream.Collectors.toList;

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

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.internal.maven.MavenDeployableProjectModelBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
public class MavenDeployableProjectModelBuilderTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expected = none();

  @Test
  @Issue("W-12036240")
  public void createDeployableProjectModelForAnAppWithOnlyAMuleConfig() throws Exception {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel("apps/simple-app");

    assertThat(deployableProjectModel.getDependencies(), hasSize(0));
  }

  @Test
  public void createDeployableProjectModelForAnEmptyAppMustFail() throws Exception {
    expected.expect(MuleRuntimeException.class);
    expected.expectMessage("src/main/mule cannot be empty");
    getDeployableProjectModel("apps/empty-app");
  }

  @Test
  public void createBasicDeployableProjectModel() throws Exception {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel("apps/basic");

    assertThat(deployableProjectModel.getPackages(), contains("org.test"));
    assertThat(deployableProjectModel.getResources(),
               containsInAnyOrder("test-script.dwl", "app.xml"));

    assertContainsResourcePaths(deployableProjectModel,
                                Paths.get("src", "main", "resources"),
                                Paths.get("src", "main", "mule"));

    assertThat(deployableProjectModel.getDependencies(), hasSize(3));
  }

  @Test
  public void createBasicDeployableProjectModelWithEmptyArtifactExportingAll() throws Exception {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel("apps/basic-with-empty-artifact-json", true);

    assertThat(deployableProjectModel.getPackages(), contains("org.test"));
    assertThat(deployableProjectModel.getResources(),
               containsInAnyOrder("test-script.dwl", "app.xml"));

    assertContainsResourcePaths(deployableProjectModel,
                                Paths.get("src", "main", "resources"),
                                Paths.get("src", "main", "mule"));

    assertThat(deployableProjectModel.getDependencies(), hasSize(3));

    Map<String, Object> exportedResourcesAndClasses =
        deployableProjectModel.getDeployableModel().getClassLoaderModelLoaderDescriptor().getAttributes();

    List<String> exportedResources = (List<String>) exportedResourcesAndClasses.get("exportedResources");
    assertThat(exportedResources.size(), is(2));
    assertThat(exportedResources, containsInAnyOrder("test-script.dwl", "app.xml"));

    List<String> exportedClasses = (List<String>) exportedResourcesAndClasses.get("exportedPackages");
    assertThat(exportedClasses.size(), is(1));
    assertThat(exportedClasses.get(0), is("org.test"));
  }

  @Test
  public void createBasicDeployableProjectModelWithEmptyArtifactNotExportingResources() throws Exception {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel("apps/basic-with-empty-artifact-json", false);

    assertThat(deployableProjectModel.getPackages(), contains("org.test"));
    assertThat(deployableProjectModel.getResources(),
               containsInAnyOrder("test-script.dwl", "app.xml"));

    assertContainsResourcePaths(deployableProjectModel,
                                Paths.get("src", "main", "resources"),
                                Paths.get("src", "main", "mule"));

    assertThat(deployableProjectModel.getDependencies(), hasSize(3));

    assertThat(deployableProjectModel.getDeployableModel().getClassLoaderModelLoaderDescriptor().getAttributes(),
               aMapWithSize(0));
  }

  @Test
  public void createBasicDeployableProjectModelWithDifferentSource() throws Exception {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel("apps/basic-with-different-source-directory");

    assertThat(deployableProjectModel.getPackages(), contains("org.test"));
    assertThat(deployableProjectModel.getResources(),
               containsInAnyOrder("test-script.dwl", "app.xml"));

    assertContainsResourcePaths(deployableProjectModel,
                                Paths.get("src", "main2", "resources"),
                                Paths.get("src", "main2", "mule"));

    assertThat(deployableProjectModel.getDependencies(), hasSize(3));
  }

  @Test
  public void createBasicDeployableProjectModelWithCustomResources() throws Exception {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel("apps/basic-with-resources");

    assertThat(deployableProjectModel.getPackages(), contains("org.test"));
    assertThat(deployableProjectModel.getResources(),
               containsInAnyOrder("test-script.dwl", "app.xml", "test-script2.dwl"));

    assertContainsResourcePaths(deployableProjectModel,
                                Paths.get("src", "main", "resources"),
                                Paths.get("src", "main", "resources2"),
                                Paths.get("src", "main", "mule"));

    assertThat(deployableProjectModel.getDependencies(), hasSize(3));
  }

  @Test
  public void createDeployableProjectModelWithSharedLibrary() throws URISyntaxException {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel("apps/shared-lib");

    // checks there are no packages in the project
    assertThat(deployableProjectModel.getPackages(), hasSize(0));

    assertThat(deployableProjectModel.getSharedLibraries(), contains(hasProperty("artifactId", equalTo("derby"))));
  }

  @Test
  public void createDeployableProjectModelWithTransitiveSharedLibrary() throws URISyntaxException {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel("apps/shared-lib-transitive");

    assertThat(deployableProjectModel.getSharedLibraries(), hasSize(6));
    assertThat(deployableProjectModel.getSharedLibraries(), hasItem(hasProperty("artifactId", equalTo("spring-context"))));
  }

  @Test
  public void createDeployableProjectModelWithAdditionalPluginDependency() throws URISyntaxException {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel("apps/additional-plugin-dependency");

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
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel("apps/additional-plugin-dependency-and-dep");

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
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel("apps/additional-plugin-dependency-transitive");

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
  public void createDeployableProjectModelIncludingTestDependencies() throws Exception {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel("apps/include-test-dependencies", false, true);

    assertThat(deployableProjectModel.getDependencies(), hasSize(1));
    assertThat(deployableProjectModel.getDependencies(),
               hasItem(hasProperty("descriptor", hasProperty("artifactId", equalTo("mule-jms-connector")))));
  }

  @Test
  @Description("Tests that even if the \"includeTestDependencies\" field is present in the class loader model loader " +
      "descriptor in the mule-artifact.json file, test dependencies are not included unless explicitly stated.")
  public void createDeployableProjectModelWithoutIncludingTestDependenciesAndIncludeTestDependenciesInMuleArtifactJson()
      throws Exception {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel("apps/include-test-dependencies");

    assertThat(deployableProjectModel.getDependencies(), hasSize(0));
  }

  @Test
  @Issue("W-12422216")
  public void createDeployableProjectModelWithConfigs() throws Exception {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel("apps/configs-not-in-model");

    assertThat(deployableProjectModel.getDeployableModel().getConfigs(), containsInAnyOrder("config.xml", "other-config.xml"));
    assertThat(deployableProjectModel.getDeployableModel().getClassLoaderModelLoaderDescriptor().getAttributes(),
               aMapWithSize(0));
  }

  @Test
  @Issue("W-12422216")
  public void configsAreHonouredIfProvided() throws Exception {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel("apps/configs-in-model");

    assertThat(deployableProjectModel.getDeployableModel().getConfigs(), containsInAnyOrder("config.xml"));
  }

  private DeployableProjectModel getDeployableProjectModel(String deployablePath,
                                                           boolean exportAllResourcesAndPackagesIfEmptyLoaderDescriptor,
                                                           boolean includeTestDependencies)
      throws URISyntaxException {
    DeployableProjectModel model =
        new MavenDeployableProjectModelBuilder(getDeployableFolder(deployablePath),
                                               exportAllResourcesAndPackagesIfEmptyLoaderDescriptor, includeTestDependencies)
                                                   .build();

    model.validate();

    return model;
  }

  private DeployableProjectModel getDeployableProjectModel(String deployablePath,
                                                           boolean exportAllResourcesAndPackagesIfEmptyLoaderDescriptor)
      throws URISyntaxException {
    return getDeployableProjectModel(deployablePath, exportAllResourcesAndPackagesIfEmptyLoaderDescriptor, false);
  }

  private DeployableProjectModel getDeployableProjectModel(String deployablePath) throws URISyntaxException {
    return getDeployableProjectModel(deployablePath, false, false);
  }

  protected File getDeployableFolder(String appPath) throws URISyntaxException {
    return new File(getClass().getClassLoader().getResource(appPath).toURI());
  }

  private void assertContainsResourcePaths(DeployableProjectModel deployableProjectModel, Path... expectedResourcePaths) {
    Path projectFolderPath = deployableProjectModel.getProjectFolder().toPath();

    List<Path> actualResourcePaths = deployableProjectModel.getResourcesPath().stream()
        .map(projectFolderPath::relativize)
        .collect(toList());

    assertThat(actualResourcePaths, containsInAnyOrder(expectedResourcePaths));
  }

}
