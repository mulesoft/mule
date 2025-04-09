/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.deployable;

import static org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModelBuilder.forMavenProject;
import static org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModelBuilder.forMavenRefreshProject;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.SupportedJavaVersions.JAVA_VERSIONS_IN_DEPLOYABLE_ARTIFACT;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.CLASSLOADING_ISOLATION;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.ARTIFACT_DESCRIPTORS;

import static java.util.Arrays.asList;
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
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;
import static org.junit.rules.ExpectedException.none;

import org.mule.runtime.module.artifact.activation.api.ArtifactActivationException;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.internal.maven.MavenDeployableProjectModelBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(CLASSLOADING_ISOLATION)
@Story(ARTIFACT_DESCRIPTORS)
@RunWith(Parameterized.class)
public class MavenDeployableProjectModelBuilderTestCase extends AbstractMuleTestCase {

  @Parameters(name = "{0}")
  public static Collection<String> data() {
    return asList("apps", "domains");
  }

  @Rule
  public ExpectedException expected = none();

  private final String deploymentTypePrefix;

  public MavenDeployableProjectModelBuilderTestCase(String deploymentTypePrefix) {
    this.deploymentTypePrefix = deploymentTypePrefix;
  }

  @Test
  @Issue("W-12036240")
  public void createDeployableProjectModelForADeploymentWithOnlyAMuleConfig() throws Exception {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel(deploymentTypePrefix + "/simple-app");

    assertThat(deployableProjectModel.getDependencies(), hasSize(0));
  }

  @Test
  public void createDeployableProjectModelForAnEmptyDeploymentMustFail() throws Exception {
    expected.expect(ArtifactActivationException.class);
    String expectedString = "src" + File.separator + "main" + File.separator + "mule cannot be empty";
    expected.expectCause(hasMessage(containsString(expectedString)));
    String deployablePath = deploymentTypePrefix + File.separator + "empty-app";
    getDeployableProjectModel(deployablePath);
  }

  @Test
  public void createBasicDeployableProjectModel() throws Exception {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel(deploymentTypePrefix + "/basic");
    validateBasicProjectModel(deployableProjectModel);
  }

  @Test
  public void forMavenProjectTest() throws URISyntaxException {
    DeployableProjectModel deployableProjectModel =
        forMavenProject(getDeployableFolder(deploymentTypePrefix + "/basic"), false, false).build();
    validateBasicProjectModel(deployableProjectModel);
  }

  @Test
  public void forMavenProjectWithNullMavenConfigTest() throws URISyntaxException {
    DeployableProjectModel deployableProjectModel =
        forMavenProject(getDeployableFolder(deploymentTypePrefix + "/basic"), null, false, false).build();
    validateBasicProjectModel(deployableProjectModel);
  }

  @Test
  public void forMavenRefreshProjectTest() throws URISyntaxException {
    DeployableProjectModel deployableProjectModel =
        forMavenProject(getDeployableFolder(deploymentTypePrefix + "/basic"), null, false, false).build();
    DeployableProjectModel refreshedDeployableProjectModel =
        forMavenRefreshProject(deployableProjectModel.getProjectStructure().get(), deployableProjectModel.getDescriptor(), false,
                               deployableProjectModel.getDependencies(), deployableProjectModel.getSharedLibraries(),
                               deployableProjectModel.getAdditionalPluginDependencies())
            .build();
    validateBasicProjectModel(refreshedDeployableProjectModel);
  }

  @Test
  public void forMavenRefreshProjectWithNullMavenConfigTest() throws URISyntaxException {
    DeployableProjectModel deployableProjectModel =
        forMavenProject(getDeployableFolder(deploymentTypePrefix + "/basic"), null, false, false).build();
    DeployableProjectModel refreshedDeployableProjectModel =
        forMavenRefreshProject(deployableProjectModel.getProjectStructure().get(), deployableProjectModel.getDescriptor(), false,
                               deployableProjectModel.getDependencies(), deployableProjectModel.getSharedLibraries(),
                               deployableProjectModel.getAdditionalPluginDependencies(), null)
            .build();
    validateBasicProjectModel(refreshedDeployableProjectModel);
  }

  private void validateBasicProjectModel(DeployableProjectModel deployableProjectModel) {
    deployableProjectModel.validate();

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
    DeployableProjectModel deployableProjectModel =
        getDeployableProjectModel(deploymentTypePrefix + "/basic-with-empty-artifact-json", true);

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
    DeployableProjectModel deployableProjectModel =
        getDeployableProjectModel(deploymentTypePrefix + "/basic-with-empty-artifact-json", false);

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
    DeployableProjectModel deployableProjectModel =
        getDeployableProjectModel(deploymentTypePrefix + "/basic-with-different-source-directory");

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
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel(deploymentTypePrefix + "/basic-with-resources");

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
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel(deploymentTypePrefix + "/shared-lib");

    // checks there are no packages in the project
    assertThat(deployableProjectModel.getPackages(), hasSize(0));

    assertThat(deployableProjectModel.getSharedLibraries(), contains(hasProperty("artifactId", equalTo("derby")),
                                                                     hasProperty("artifactId", equalTo("derbyshared"))));
  }

  @Test
  public void createDeployableProjectModelWithTransitiveSharedLibrary() throws URISyntaxException {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel(deploymentTypePrefix + "/shared-lib-transitive");

    assertThat(deployableProjectModel.getSharedLibraries(), hasSize(8));
    assertThat(deployableProjectModel.getSharedLibraries(), hasItem(hasProperty("artifactId", equalTo("spring-context"))));
  }

  @Test
  public void createDeployableProjectModelWithAdditionalPluginDependency() throws URISyntaxException {
    DeployableProjectModel deployableProjectModel =
        getDeployableProjectModel(deploymentTypePrefix + "/additional-plugin-dependency");

    assertThat(deployableProjectModel.getDependencies(), hasSize(3));
    assertThat(deployableProjectModel.getDependencies(),
               hasItem(hasProperty("descriptor", hasProperty("artifactId", equalTo("mule-db-connector")))));

    assertThat(deployableProjectModel.getAdditionalPluginDependencies(), aMapWithSize(1));
    assertThat(deployableProjectModel.getAdditionalPluginDependencies(),
               hasEntry(hasProperty("artifactId", equalTo("mule-db-connector")),
                        contains(hasProperty("descriptor", hasProperty("artifactId", equalTo("derby"))),
                                 hasProperty("descriptor", hasProperty("artifactId", equalTo("derbyshared"))))));
  }

  @Test
  public void createDeployableProjectModelWithAdditionalPluginDependencyAndDependency() throws URISyntaxException {
    DeployableProjectModel deployableProjectModel =
        getDeployableProjectModel(deploymentTypePrefix + "/additional-plugin-dependency-and-dep");

    assertThat(deployableProjectModel.getDependencies(), hasSize(5));
    assertThat(deployableProjectModel.getDependencies(),
               hasItems(hasProperty("descriptor", hasProperty("artifactId", equalTo("derby"))),
                        hasProperty("descriptor", hasProperty("artifactId", equalTo("derbyshared"))),
                        hasProperty("descriptor", hasProperty("artifactId", equalTo("mule-db-connector")))));

    assertThat(deployableProjectModel.getAdditionalPluginDependencies(), aMapWithSize(1));
    assertThat(deployableProjectModel.getAdditionalPluginDependencies(),
               hasEntry(hasProperty("artifactId", equalTo("mule-db-connector")),
                        contains(hasProperty("descriptor", hasProperty("artifactId", equalTo("derby"))),
                                 hasProperty("descriptor", hasProperty("artifactId", equalTo("derbyshared"))))));
  }

  @Test
  public void createDeployableProjectModelWithTransitiveAdditionalPluginDependency() throws URISyntaxException {
    DeployableProjectModel deployableProjectModel =
        getDeployableProjectModel(deploymentTypePrefix + "/additional-plugin-dependency-transitive");

    assertThat(deployableProjectModel.getDependencies(), hasSize(1));
    assertThat(deployableProjectModel.getDependencies(),
               hasItem(hasProperty("descriptor", hasProperty("artifactId", equalTo("mule-spring-module")))));

    assertThat(deployableProjectModel.getAdditionalPluginDependencies(), aMapWithSize(1));
    assertThat(deployableProjectModel.getAdditionalPluginDependencies(),
               hasEntry(hasProperty("artifactId", equalTo("mule-spring-module")), hasSize(8)));
    assertThat(deployableProjectModel.getAdditionalPluginDependencies(),
               hasEntry(hasProperty("artifactId", equalTo("mule-spring-module")),
                        hasItem(hasProperty("descriptor", hasProperty("artifactId", equalTo("spring-context"))))));
  }

  @Test
  public void createDeployableProjectModelIncludingTestDependencies() throws Exception {
    DeployableProjectModel deployableProjectModel =
        getDeployableProjectModel(deploymentTypePrefix + "/include-test-dependencies", false, true);

    assertThat(deployableProjectModel.getDependencies(), hasSize(1));
    assertThat(deployableProjectModel.getDependencies(),
               hasItem(hasProperty("descriptor", hasProperty("artifactId", equalTo("mule-jms-connector")))));
  }

  @Test
  @Description("Tests that even if the \"includeTestDependencies\" field is present in the class loader model loader " +
      "descriptor in the mule-artifact.json file, test dependencies are not included unless explicitly stated.")
  public void createDeployableProjectModelWithoutIncludingTestDependenciesAndIncludeTestDependenciesInMuleArtifactJson()
      throws Exception {
    DeployableProjectModel deployableProjectModel =
        getDeployableProjectModel(deploymentTypePrefix + "/include-test-dependencies");

    assertThat(deployableProjectModel.getDependencies(), hasSize(0));
  }

  @Test
  @Issue("W-12422216")
  public void createDeployableProjectModelWithConfigs() throws Exception {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel(deploymentTypePrefix + "/configs-not-in-model");

    assertThat(deployableProjectModel.getDeployableModel().getConfigs(), containsInAnyOrder("config.xml", "other-config.xml"));
    assertThat(deployableProjectModel.getDeployableModel().getClassLoaderModelLoaderDescriptor().getAttributes(),
               aMapWithSize(0));
  }

  @Test
  @Issue("W-12422216")
  public void configsAreHonouredIfProvided() throws Exception {
    DeployableProjectModel deployableProjectModel = getDeployableProjectModel(deploymentTypePrefix + "/configs-in-model");

    assertThat(deployableProjectModel.getDeployableModel().getConfigs(), containsInAnyOrder("config.xml"));
  }

  @Test
  @Story(JAVA_VERSIONS_IN_DEPLOYABLE_ARTIFACT)
  public void supportedJavaVersions() throws Exception {
    DeployableProjectModel deployableProjectModel =
        getDeployableProjectModel(deploymentTypePrefix + "/basic-with-supported-java-versions");

    assertThat(deployableProjectModel.getDeployableModel().getSupportedJavaVersions(), hasItems("1.8", "11", "17"));
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

  protected File getDeployableFolder(String deployableArtifactPath) throws URISyntaxException {
    return new File(getClass().getClassLoader().getResource(deployableArtifactPath).toURI());
  }

  private void assertContainsResourcePaths(DeployableProjectModel deployableProjectModel, Path... expectedResourcePaths) {
    Path projectFolderPath = deployableProjectModel.getProjectFolder().toPath();

    List<Path> actualResourcePaths = deployableProjectModel.getResourcesPath().stream()
        .map(projectFolderPath::relativize)
        .collect(toList());

    assertThat(actualResourcePaths, containsInAnyOrder(expectedResourcePaths));
  }

}
